/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.events;

import io.redlink.smarti.model.Conversation;

/**
 * An event that the processing of a Conversation is complete
 */
public class ConversationProcessCompleteEvent {

    private final Conversation conversation;

    public ConversationProcessCompleteEvent(Conversation conversation) {
        this.conversation = conversation;
    }

    public Conversation getConversation() {
        return conversation;
    }
}
