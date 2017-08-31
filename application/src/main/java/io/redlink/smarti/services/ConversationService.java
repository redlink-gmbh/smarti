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

package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.events.ConversationProcessCompleteEvent;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    private QueryBuilderService queryBuilderService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ConversationRepository conversationRepository;

    private final ExecutorService processingExecutor;

    public ConversationService(Optional<ExecutorService> processingExecutor) {
        this.processingExecutor = processingExecutor.orElseGet(() -> Executors.newFixedThreadPool(2));
    }

    public Conversation appendMessage(Conversation conversation, Message message, boolean process, Consumer<Conversation> onCompleteCallback) {
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
                    
                    queryBuilderService.buildQueries(finalConversation);

                    try {
                        final Conversation storedConversation = storeService.storeIfUnmodifiedSince(finalConversation, lastModified);

                        if (log.isDebugEnabled()) {
                            logConversation(storedConversation);
                        }

                        eventPublisher.publishEvent(new ConversationProcessCompleteEvent(storedConversation));

                        if (onCompleteCallback != null) {
                            onCompleteCallback.accept(storedConversation);
                        }
                    } catch (ConcurrentModificationException e) {
                        log.debug("Conversation {} has been modified while analysis was in progress", finalConversation.getId());
                    }
                } catch (Throwable t) {
                    log.error("Error during async prepare: {}", t.getMessage(), t);
                }
            });
        }

        return conversation;
    }

    private void logConversation(Conversation c) {
        if(!log.isDebugEnabled()) return;
        log.debug("Conversation[id:{} | channel: {} | modified: {}]", c.getId(), c.getChannelId(),
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
                log.debug("    {}. {}", count.getAndIncrement(), t);
                if (CollectionUtils.isNotEmpty(t.getQueries())) {
                    log.debug("    > with {} queries", t.getQueries().size());
                    t.getQueries().forEach(q -> log.debug("       - {}", q));
                }
            });
        }
    }

    public Conversation appendMessage(Conversation conversation, Message message, Consumer<Conversation> onCompleteCallback) {
        return appendMessage(conversation, message, true, onCompleteCallback);
    }

    public Conversation appendMessage(Conversation conversation, Message message) {
        return appendMessage(conversation, message, true, null);
    }

    public Conversation completeConversation(Conversation conversation) {
        return storeService.completeConversation(conversation.getId());
    }

    public Conversation rateMessage(Conversation conversation, String messageId, int delta) {
        return storeService.adjustMessageVotes(conversation.getId(), messageId, delta);
    }

    public List<? extends Result> getInlineResults(Conversation conversation, Template template, String creator) throws IOException {
        return queryBuilderService.execute(creator, template, conversation);
    }

    public Page<Conversation> listConversations(String clientId, int page, int pageSize) {
        final PageRequest paging = new PageRequest(page, pageSize);
        if (StringUtils.isNotBlank(clientId)) {
            return conversationRepository.findByClientId(clientId, paging);
        } else {
            return conversationRepository.findAll(paging);
        }
    }

    public Conversation getConversation(ObjectId conversationId) {
        return storeService.get(conversationId);
    }
}
