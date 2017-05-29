/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.api.event;

import io.redlink.smarti.model.ConversationMeta;

import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEvent;

/**
 */
public class StoreServiceEvent extends ApplicationEvent {

    public enum Operation {
        SAVE,
        DELETE
    }

    private final Operation operation;
    private final ObjectId conversationId;
    private final ConversationMeta.Status conversationStatus;

    public StoreServiceEvent(Object source, Operation operation, ObjectId conversationId, ConversationMeta.Status conversationStatus) {
        super(source);
        this.operation = operation;
        this.conversationId = conversationId;
        this.conversationStatus = conversationStatus;
    }

    public ObjectId getConversationId() {
        return conversationId;
    }

    public ConversationMeta.Status getConversationStatus() {
        return conversationStatus;
    }

    public Operation getOperation() {
        return operation;
    }

    public static  StoreServiceEvent save(ObjectId conversationId, Object source) {
        return new StoreServiceEvent(source, Operation.SAVE, conversationId, null);
    }

    public static  StoreServiceEvent save(ObjectId conversationId, ConversationMeta.Status conversationStatus, Object source) {
        return new StoreServiceEvent(source, Operation.SAVE, conversationId, conversationStatus);
    }

    public static  StoreServiceEvent delete(ObjectId conversationId, Object source) {
        return new StoreServiceEvent(source, Operation.DELETE, conversationId, null);
    }


}
