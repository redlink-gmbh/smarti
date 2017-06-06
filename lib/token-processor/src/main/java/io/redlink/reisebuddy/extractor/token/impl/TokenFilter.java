/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.reisebuddy.extractor.token.impl;

import org.springframework.stereotype.Component;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.ner.NerSet;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Token;

import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;

import java.util.*;

/**
 * {@link QueryPreparator} that filters overlapping Tokens of
 * the same type. This happens if multiple QueryPreparators
 * do mark the same mention in the conversation as an Token with
 * the same {@link Token.Type}.
 * <p>
 * {@link Hint}s of filtered {@link Token}s are copied over to the
 * token that is kept.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class TokenFilter extends Processor {

    protected TokenFilter() {
        super("token.filter","Token Filter",Phase.post,10); //the last one
    }

    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap();
    }
    
    @Override
    protected void init() throws Exception {
        //no op
    }
    
    @Override
    public void doProcessing(ProcessingData processingData) {
        Conversation c = processingData.getAnnotation(CONVERSATION_ANNOTATION);
        if(c == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, CONVERSATION_ANNOTATION);
            return;
        }
        //(1) we need to find the first token created by this analysis step
        int lastAnalyzed = c.getMeta().getLastMessageAnalyzed();
        final List<Token> newTokens;
        if(lastAnalyzed >= 0){ //we need to find the first new token
            List<Token> tokens = c.getTokens();
            int firstNewTokenIdx = 0;
            for(int i = tokens.size() - 1; i >= 0; i--){
                Token token = tokens.get(i);
                if(token.getMessageIdx() <= lastAnalyzed){
                    firstNewTokenIdx = i + 1;
                    break;
                }
            }
            newTokens = tokens.subList(firstNewTokenIdx, tokens.size());
        } else { //all tokens are new
            newTokens = c.getTokens();
        }
        //(2) sort the new tokens
        Collections.sort(newTokens, Token.IDX_START_END_COMPARATOR);
        //(3) filter for tokens contained in an other token with the same type
        Map<Token.Type, Token> activeTokens = new EnumMap<>(Token.Type.class);
        for(Iterator<Token> it = newTokens.iterator(); it.hasNext();){
            Token token = it.next();
            Token active = activeTokens.get(token.getType());
            if(active == null || active.getMessageIdx() != token.getMessageIdx() ||
                    token.getEnd() > active.getEnd() || !token.getValue().equals(active.getValue())){
                activeTokens.put(token.getType(), token);
            } else {
                log.debug("filter token {} contained in {}", token, active);
                it.remove();
                for(String hint : token.getHints()){
                    if(active.addHint(hint)){
                        log.debug("  ... copied Hint {} from contained Token", hint);
                    }
                }
            }
        }
    }

}
