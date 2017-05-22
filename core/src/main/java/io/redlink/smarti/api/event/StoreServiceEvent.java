/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.api.event;

import io.redlink.smarti.model.ConversationMeta;
import org.springframework.context.ApplicationEvent;

/**
 */
public class StoreServiceEvent extends ApplicationEvent {

    public enum Operation {
        SAVE,
        DELETE
    }

    private final Operation operation;
    private final String conversationId;
    private final ConversationMeta.Status conversationStatus;

    public StoreServiceEvent(Object source, Operation operation, String conversationId, ConversationMeta.Status conversationStatus) {
        super(source);
        this.operation = operation;
        this.conversationId = conversationId;
        this.conversationStatus = conversationStatus;
    }

    public String getConversationId() {
        return conversationId;
    }

    public ConversationMeta.Status getConversationStatus() {
        return conversationStatus;
    }

    public Operation getOperation() {
        return operation;
    }

    public static  StoreServiceEvent save(String conversationId, Object source) {
        return new StoreServiceEvent(source, Operation.SAVE, conversationId, null);
    }

    public static  StoreServiceEvent save(String conversationId, ConversationMeta.Status conversationStatus, Object source) {
        return new StoreServiceEvent(source, Operation.SAVE, conversationId, conversationStatus);
    }

    public static  StoreServiceEvent delete(String conversationId, Object source) {
        return new StoreServiceEvent(source, Operation.DELETE, conversationId, null);
    }


}
