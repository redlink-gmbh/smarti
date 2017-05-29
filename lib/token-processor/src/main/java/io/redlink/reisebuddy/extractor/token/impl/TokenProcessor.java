/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.reisebuddy.extractor.token.impl;

import io.redlink.reisebuddy.api.QueryPreparator;
import io.redlink.reisebuddy.extractor.token.TokenProcessingRuleset;
import io.redlink.reisebuddy.model.*;
import io.redlink.reisebuddy.model.Message.Origin;
import io.redlink.reisebuddy.model.Token.Type;
import io.redlink.reisebuddy.processing.ProcessingData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link QueryPreparator} that applies all the registered
 * {@link TokenProcessingRuleset}s. This runs in {@link Phase#post}
 * @author Rupert Westenthaler
 *
 */
@Component
public class TokenProcessor extends QueryPreparator {

    /**
     * The minimum confidence for topics (currently none)
     */
    private static final float MIN_TOPIC_CONF = 0f;
    private final Collection<TokenProcessingRuleset> rulesets;

    @Autowired
    public TokenProcessor(Collection<TokenProcessingRuleset> rulesets) {
        super(Phase.post);
        this.rulesets = rulesets;
    }
    
    @Override
    public void prepare(ProcessingData processingData) {
        //(1) filter the rule sets based on the language of the conversation
        String language = processingData.getLanguage();
        List<TokenProcessingRuleset> rulesets = this.rulesets.stream()
                .filter(rs -> rs.getLanguage() == null || language.equals(rs.getLanguage()))
                .collect(Collectors.toList());
        if(rulesets.isEmpty()){
            return; //no ruleset for this language
        }
        
        final Conversation conv = processingData.getConversation();
        final List<Message> messages = conv.getMessages();
        final List<Token> tokens = conv.getTokens();

        //(2) get the messages we want to process
        int lastAnalyzed = conv.getMeta().getLastMessageAnalyzed();
        for(int i = lastAnalyzed + 1; i < messages.size(); i++){
            final int mIdx = i;
            final Message message = messages.get(mIdx);
            if(log.isDebugEnabled()){
                log.debug(" - Message {}:{}", mIdx,StringUtils.abbreviate(message.getContent(), 50));
            }
            if(message.getOrigin() == Origin.User){
                List<Token> msgTokens = tokens.stream() //the tokes of the current message
                        .filter(t -> t.getMessageIdx() == mIdx)
                        .filter(t -> t.getState() != State.Rejected)
                        .collect(Collectors.toList());
                //transitive closure over the MessageTopics of the current message
                EnumSet<MessageTopic> topics = msgTokens.stream()
                        .filter(t -> t.getType() == Type.Topic)
                        .filter(t -> t.getConfidence() >= MIN_TOPIC_CONF)
                        .map(Token::getValue)
                        .filter(v -> v instanceof MessageTopic)
                        .map(MessageTopic.class::cast)
                        .map(MessageTopic::hierarchy)
                        .flatMap(s -> s.stream())
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(MessageTopic.class)));
                //(3) apply the matching rule sets
                for(TokenProcessingRuleset ruleset : rulesets){
                    if(!Collections.disjoint(topics, ruleset.topics())){
                        List<Token> rulesetTokens = msgTokens.stream()
                                .filter(t -> ruleset.tokenTypes().contains(t.getType()))
                                .collect(Collectors.toList());
                        if(!rulesetTokens.isEmpty()){
                            log.debug("   apply {} ",ruleset.getClass().getSimpleName());
                            ruleset.apply(message, rulesetTokens);
                        }
                    }
                }
            } //else ignore agent messages
        }
    }

    
    
}
