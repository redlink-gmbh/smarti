/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.events.ConversationProcessCompleteEvent;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Conversation-related services
 */
@Service
public class ConversationService {

    @Autowired
    private StoreService storeService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ExecutorService processingExecutor;

    public ConversationService(Optional<ExecutorService> processingExecutor) {
        this.processingExecutor = processingExecutor.orElseGet(() -> Executors.newFixedThreadPool(2));
    }

    public Conversation appendMessage(Conversation conversation, Message message, boolean process) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(message);

        conversation = storeService.appendMessage(conversation, message);

        if (process) {
            final Conversation finalConversation = conversation;
            processingExecutor.submit(() -> {
                // TODO: call the prepare/query methods

                eventPublisher.publishEvent(new ConversationProcessCompleteEvent(finalConversation));
            });
        }

        return conversation;
    }

    public Conversation appendMessage(Conversation conversation, Message message) {
        return appendMessage(conversation, message, true);
    }

}
