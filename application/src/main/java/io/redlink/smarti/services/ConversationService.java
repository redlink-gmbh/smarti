/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.events.ConversationProcessCompleteEvent;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Token;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    private TemplateService templateService;
    
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

                    templateService.updateTemplates(finalConversation);

                    storeService.storeIfUnmodifiedSince(finalConversation, lastModified);

                    if(log.isDebugEnabled()){
                        logConversation(finalConversation);
                    }
                    
                    eventPublisher.publishEvent(new ConversationProcessCompleteEvent(finalConversation));
                    
                } catch (Throwable t) {
                    log.error("Error during async prepare: {}", t.getMessage(), t);
                }
            });
        }

        return conversation;
    }

    private void logConversation(Conversation c) {
        if(!log.isDebugEnabled()) return;
        log.debug("Conversation[id:{} | channel: {} | modified: {}]",c.getId(), c.getChannelId(),
                c.getLastModified() != null ? DateFormatUtils.ISO_DATETIME_FORMAT.format(c.getLastModified()) : "unknown");
        if(c.getUser() != null){
            log.debug(" > user[id: {}| name: {}] ", c.getUser().getId(), c.getUser().getDisplayName());
        }
        if(c.getMessages() != null){
            log.debug(" > {} messages:", c.getMessages().size());
            AtomicInteger count = new AtomicInteger(0);
            c.getMessages().forEach(m -> {
                log.debug("    {}. {} : {}",count.incrementAndGet(), m.getUser() == null ? m.getOrigin() : m.getUser().getDisplayName(), m.getContent());
            });
        }
        if(c.getTokens() != null){
            log.debug(" > {} tokens:", c.getTokens().size());
            AtomicInteger count = new AtomicInteger(0);
            c.getTokens().forEach(t -> {
                log.debug("    {}. {}",count.getAndIncrement(), t);
            });
        }
        if(c.getTemplates() != null){
            log.debug(" > {} templates:", c.getTemplates().size());
            AtomicInteger count = new AtomicInteger(0);
            c.getTemplates().forEach(t -> {
                log.debug("    {}. {}",count.getAndIncrement(), t);
            });
        }
    }

    public Conversation appendMessage(Conversation conversation, Message message) {
        return appendMessage(conversation, message, true);
    }

    public Conversation completeConversation(Conversation conversation) {
        return storeService.completeConversation(conversation.getId());
    }
}
