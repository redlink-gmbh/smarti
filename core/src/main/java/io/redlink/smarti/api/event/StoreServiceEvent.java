/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.api.event;

import io.redlink.smarti.model.ConversationMeta;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEvent;

/**
 */
public class StoreServiceEvent extends ApplicationEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 2993017858379839079L;

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

    @Override
    public String toString() {
        return "StoreServiceEvent [operation=" + operation + ", conversationId=" + conversationId
                + ", conversationStatus=" + conversationStatus + "]";
    }


}
