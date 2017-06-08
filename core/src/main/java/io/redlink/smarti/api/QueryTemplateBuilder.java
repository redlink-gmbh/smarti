/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.api;

import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public abstract class QueryTemplateBuilder {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The minimum required configuration for a {@link MessageTopic} to be considered (currently <code>0</code>)
     */
    private static final float MIN_TOPIC_CONF = 0;

    /**
     * The {@link IntendDefinition} used for {@link Intent}s build by this {@link QueryTemplateBuilder} implementation
     * @return the query template definition. MUST NOT be <code>null</code>
     */
    protected abstract IntendDefinition getDefinition();

    
//    protected abstract Set<MessageTopic> getSupportedTopics();

    /**
     * Updates an existing template based on the current state of the conversation
     * @param template the template to update
     * @param conversation the conversation
     * @param startMsgIdx the template MUST only consider Tokens with {@link Token.Origin#System} that are
     * extracted from messages with a &gt;= index as the parsed value. NOTE: that for {@link Token.Origin#Agent}
     * origin Tokens the index will be <code>-1</code>. Those tokens should also be considered.
     * @return the Integer token index of tokens used to update the template. In other words the
     * token indexes of tokens newly referenced by {@link Slot}s of the parsed {@link Intent}
     */
    protected abstract Set<Integer> updateTemplate(Intent template, Conversation conversation, int startMsgIdx);

    /**
     * Requests the creation of a {@link Intent} for the {@link IntendDefinition}
     * supported by this implementation to the parsed Conversation
     * @param conversation the conversation
     * @param probability the probability of the {@link IntendDefinition#getType()}
     * @param startMsgIdx the template MUST only consider Tokens with {@link Token.Origin#System} that are
     * extracted from messages with a &gt;= index as the parsed value. NOTE: that for {@link Token.Origin#Agent}
     * origin Tokens the index will be <code>-1</code>. Those tokens should also be considered.
     */
    private void createTemplate(Conversation conversation, int startMsgIdx, float probability, int[] messageTokenIndexes) {
        final Intent queryTemplate = new Intent(getDefinition().getType(), new HashSet<>());
        queryTemplate.setProbability(probability);

        for (int mesageTokenIndex : messageTokenIndexes) {
            Slot topicSlot = getDefinition().createSlot(IntendDefinition.TOPIC);
            topicSlot.setTokenIndex(mesageTokenIndex);
            queryTemplate.getSlots().add(topicSlot);
        }

        initializeTemplate(queryTemplate);

        updateTemplate(queryTemplate, conversation, startMsgIdx);



        conversation.getQueryTemplates().add(queryTemplate);
    }

    protected abstract void initializeTemplate(Intent queryTemplate);

    /**
     * Builds and updates templates for the parsed conversation based on
     * tokens extracted starting from the parsed message index
     * @param conversation the conversation
     */
    public final void buildTemplate(Conversation conversation, int startMsgIdx) {
        //first check if we can update an existing query templates
        for(Intent template : conversation.getQueryTemplates()){
            if(getDefinition().getType() == template.getType()){// &&
                    //TODO: Maybe we would like to update valid templates
                    //!getDefinition().isValid(template, conversation.getTokens())){
                Set<Integer> addedTokens = updateTemplate(template, conversation,startMsgIdx);
                //TODO: check if this assumption is correct
                if(!addedTokens.isEmpty()){
                    return; //updated existing template ... do not build a new one (of the same type)
                }
            } //else not matching or already valid template
        }

        final int[] consumedTypeTokens = conversation.getQueryTemplates().stream()
                .flatMap(t -> t.getSlots().stream())
                .filter(s -> IntendDefinition.TOPIC.equals(s.getRole()))
                .mapToInt(Slot::getTokenIndex)
                .filter(i -> i >= 0)
                .distinct()
                .sorted()
                .toArray();

        //second try to create new query templates
        final int lastAnalyzed = conversation.getMeta().getLastMessageAnalyzed();
        //MessageTopics for the current message
        final AtomicInteger idx = new AtomicInteger(0);
        final int[] consumableMessageTokenIndexes = conversation.getTokens().stream().sequential()
                .map(t -> ImmutablePair.of(idx.getAndIncrement(), t))
                .filter(p -> Arrays.binarySearch(consumedTypeTokens, p.getLeft()) < 0)
                .filter(p -> p.getRight().getMessageIdx() >= lastAnalyzed) //TODO: we need a better way to determine "new" Tokens
                .filter(p -> p.getRight().getState() != State.Rejected)
                .filter(p -> p.getRight().getType() == Type.Topic)
                .filter(p -> p.getRight().getConfidence() >= MIN_TOPIC_CONF)
                .filter(p -> p.getRight().getValue() instanceof MessageTopic)
                .filter(p -> ((MessageTopic) p.getRight().getValue()).hierarchy().contains(getDefinition().getType()))
                .mapToInt(ImmutablePair::getLeft)
                .sorted()
                .toArray();
        if (consumableMessageTokenIndexes.length != 0) {
            float conf = 0f;
            for (int i : consumableMessageTokenIndexes) {
                if (conversation.getTokens().get(i).getConfidence() > conf) {
                    conf = conversation.getTokens().get(i).getConfidence();
                }
            }
            createTemplate(conversation, startMsgIdx, conf, consumableMessageTokenIndexes);
        }  //no match with the topics of the current message
    }

    /**
     * Logs information about the query template
     * @param queryTemplate
     * @param tokens
     */
    protected final void debugQueryTemplate(Intent queryTemplate, List<Token> tokens) {
        if(log.isDebugEnabled()){
            log.debug("Built QueryTemplate for {}",queryTemplate.getType());
            for(Slot slot : queryTemplate.getSlots()){
                log.debug(" - {}: {}",slot.getRole(), slot.getTokenIndex() < 0 ? 
                        String.format("unboud <%s> %s", slot.getTokenType(), slot.isRequired() ? "required" : "optional") : 
                            tokens.get(slot.getTokenIndex()));
            }
        }
    }

}
