/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Conversation-related services
 */
@Service
public class ConversationService {

    @Autowired
    private StoreService storeService;

    public Conversation appendMessage(Conversation conversation, Message message, boolean process) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(message);

        conversation.getMessages().add(message);

        if (process) {
            // TODO: call the prepare/query methods
        }

        return storeService.store(conversation);
    }

    public Conversation appendMessage(Conversation conversation, Message message) {
        return appendMessage(conversation, message, true);
    }

}
