/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.events.ConversationProcessCompleteEvent;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Conversation-related services
 */
@Service
public class ConversationService {

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private StoreService storeService;

    @Autowired
    private PrepareService prepareService;

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
            final Date lastModified = conversation.getLastModified();
            processingExecutor.submit(() -> {
                try {
                    prepareService.prepare(finalConversation);

                    // TODO: call the prepare/query methods

                    storeService.storeIfUnmodifiedSince(finalConversation, lastModified);

                    eventPublisher.publishEvent(new ConversationProcessCompleteEvent(finalConversation));
                } catch (Throwable t) {
                    log.error("Error during async prepare: {}", t.getMessage(), t);
                }
            });
        }

        return conversation;
    }

    public Conversation appendMessage(Conversation conversation, Message message) {
        return appendMessage(conversation, message, true);
    }

    public Conversation completeConversation(Conversation conversation) {
        return conversation;
    }
}
