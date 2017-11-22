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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.events.ConversationProcessCompleteEvent;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.processing.ProcessingConfiguration;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Conversation-related services
 */
@Service
@EnableConfigurationProperties(ProcessingConfiguration.class)
public class ConversationService {

    private final Logger log = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private StoreService storeService;

    @Autowired
    private PrepareService prepareService;

    @Autowired
    private QueryBuilderService queryBuilderService;
    
    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ConfigurationService confService;

    @Autowired
    private ConversationRepository conversationRepository;

    private final ExecutorService processingExecutor;

    
    public ConversationService(ProcessingConfiguration processingConfiguration) {
        this.processingExecutor = processingConfiguration.createExecuterService();
    }
    
    /**
     * Updates the parsed conversation
     * @param client the client
     * @param conversation the conversation (MUST BE owned by the parsed client)
     * @param process if the conversation needs to be processed (done asynchronously)
     * @param onCompleteCallback called after the operation completes (including the optional asynchronous processing)
     * @return the updated conversation as stored after the update and likely before processing has completed. Use the 
     * <code>onCompleteCallback</code> to get the updated conversation including processing results
     */
    public Conversation update(Client client, Conversation conversation, boolean process, Consumer<Conversation> onCompleteCallback) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(client);
        if(!Objects.equal(client.getId(),conversation.getOwner())){
            throw new IllegalStateException("The parsed Client MUST BE the owner of the conversation!");
        }
        
        final Conversation storedConversation = storeService.store(conversation);
        if(process){
            process(client, storedConversation, onCompleteCallback);
        } else if(onCompleteCallback != null){
            onCompleteCallback.accept(storedConversation);
        }
        return conversation;
    }
    
    /**
     * Appends a message to the end of the conversation
     * @param client the client
     * @param conversation the conversation (MUST BE owned by the parsed client)
     * @param process if the conversation needs to be processed (done asynchronously)
     * @param onCompleteCallback called after the operation completes (including the optional asynchronous processing)
     * @return the updated conversation as stored after the update and likely before processing has completed. Use the 
     * <code>onCompleteCallback</code> to get the updated conversation including processing results
     */
    public Conversation appendMessage(Client client, Conversation conversation, Message message, boolean process, Consumer<Conversation> onCompleteCallback) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(client);
        if(!Objects.equal(client.getId(),conversation.getOwner())){
            throw new IllegalStateException("The parsed Client MUST BE the owner of the conversation!");
        }

        final Conversation storedConversation = storeService.appendMessage(conversation, message);

        if (process) {
            process(client, storedConversation, onCompleteCallback);
        } else {
            if(onCompleteCallback != null){
                onCompleteCallback.accept(storedConversation);
            }
        }

        return storedConversation;
    }

    /**
     * Processes the conversation for the parsed client and saved the processing results if the parsed client is also
     * the owner of the conversation AND the conversation was not updated in the meantime. 
     * @param client
     * @param conv
     * @param onCompleteCallback
     */
    private void process(Client client, final Conversation conv, Consumer<Conversation> onCompleteCallback) {
        final Date lastModified = conv.getLastModified();
        processingExecutor.submit(() -> {
            try {
                Conversation conversation = conv;
                prepareService.prepare(client, conversation);

                templateService.updateTemplates(client, conversation);
                
                queryBuilderService.buildQueries(client, conversation);

                try {
                    final Conversation result;
                    if(client != null && Objects.equal(conversation.getOwner(), client.getId())){
                        result = storeService.storeIfUnmodifiedSince(conversation, lastModified);
    
                        if (log.isDebugEnabled()) {
                            logConversation(result);
                        }
    
                        eventPublisher.publishEvent(new ConversationProcessCompleteEvent(result));
                    } else { 
                        result = conversation;
                    }
                    if (onCompleteCallback != null) {
                        onCompleteCallback.accept(result);
                    }
                } catch (ConcurrentModificationException e) {
                    log.debug("Conversation {} has been modified while analysis was in progress", conversation.getId());
                }
            } catch (Throwable t) {
                log.error("Error during async prepare: {}", t.getMessage(), t);
            }
        });
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

    public Conversation appendMessage(Client client, Conversation conversation, Message message, Consumer<Conversation> onCompleteCallback) {
        return appendMessage(client, conversation, message, true, onCompleteCallback);
    }

    public Conversation appendMessage(Client client, Conversation conversation, Message message) {
        return appendMessage(client, conversation, message, true, null);
    }

    public Conversation completeConversation(Conversation conversation) {
        return storeService.completeConversation(conversation.getId());
    }

    public Conversation rateMessage(Conversation conversation, String messageId, int delta) {
        return storeService.adjustMessageVotes(conversation.getId(), messageId, delta);
    }


    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Template template, String creator, MultiValueMap<String, String> params) throws IOException {
        return queryBuilderService.execute(client, creator, template, conversation, params);
    }


    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Template template, String creator) throws IOException {
        return getInlineResults(client, conversation, template, creator, new LinkedMultiValueMap<>());
    }
    
    public Conversation getConversation(Client client, ObjectId convId){
        Conversation conversation = storeService.get(convId);
        if(conversation == null){
            return null;
        }
        return updateQueries(client, conversation);
    }

    private Conversation updateQueries(Client client, Conversation conversation) {
        if (conversation == null) return null;

        Configuration config;
        if(client == null){
            config = confService.getClientConfiguration(conversation.getOwner());
        } else {
            config = confService.getClientConfiguration(client);
        }
        if(config == null){
            log.debug("Client {} does not have a configuration. Will use default configuration", client);
            config = confService.getDefaultConfiguration();
        }
        Date confModDate = config.getModified();
        if(confModDate == null || conversation.getLastModified().before(confModDate)){
            log.debug("update queries for {} because after configuration change",conversation);
            queryBuilderService.buildQueries(config, conversation);
            if(Objects.equal(conversation.getOwner(), config.getClient())){//only store updated queries if we used the owners conviguration
                conversation = storeService.storeIfUnmodifiedSince(conversation, conversation.getLastModified());
            } //TODO: when we add a query cache we could also cache queries for other clients as the owner of the conversation
        }
        return conversation;
    }

    public Conversation getCurrentConversationByChannelId(Client client, String channelId) {
        return getCurrentConversationByChannelId(client, channelId, Conversation::new);
    }

    public Conversation getCurrentConversationByChannelId(Client client, String channelId, Supplier<Conversation> supplier) {
        Preconditions.checkNotNull(client);
        Preconditions.checkArgument(StringUtils.isNoneBlank(channelId));
        final ObjectId conversationId = storeService.mapChannelToCurrentConversationId(channelId);
        if (conversationId != null) {
            Conversation conversation = storeService.get(conversationId);
            if(Objects.equal(conversation.getOwner(), client.getId())){
                return getConversation(client, conversationId);
            } else {
                //this should never happen unless we have two clients with the same channelId
                throw new IllegalStateException("Conversation for channel '" + channelId + "' has a different owner "
                        + "as the current client (owner: " + conversation.getOwner() + ", client: " + client + ")!");
            }
        } else {
            final Conversation c = supplier.get();
            if(c != null){
                c.setId(null);
                c.setOwner(client.getId());
                c.setChannelId(channelId);
                return storeService.store(c);
            } else {
                return null;
            }
        }
    }

    public Page<Conversation> listConversations(ObjectId clientId, int page, int pageSize) {
        final PageRequest paging = new PageRequest(page, pageSize);
        if (java.util.Objects.nonNull(clientId)) {
            return conversationRepository.findByOwner(clientId, paging);
        } else {
            return conversationRepository.findAll(paging);
        }
    }

    public Conversation getConversation(ObjectId conversationId) {
        return storeService.get(conversationId);
    }

    public List<Conversation> getConversations(ObjectId owner) {
        return conversationRepository.findByOwner(owner);
    }

    public void importConversations(ObjectId owner, List<Conversation> conversations) {
        importConversations(owner, conversations, false);
    }

    public void importConversations(ObjectId owner, List<Conversation> conversations, boolean replace) {
        // TODO(westei): implement this method
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean exists(ObjectId conversationId) {
        return conversationRepository.exists(conversationId);
    }

    public Conversation updateStatus(ObjectId conversationId, ConversationMeta.Status newStatus) {
        return publishSaveEvent(updateQueries(null, conversationRepository.updateConversationStatus(conversationId, newStatus)));
    }

    public boolean deleteMessage(ObjectId conversationId, String messageId) {
        final boolean success = conversationRepository.deleteMessage(conversationId, messageId);
        if (success) {
            final Conversation one = conversationRepository.findOne(conversationId);
            publishSaveEvent(updateQueries(null, one));
        }
        return success;
    }

    public Conversation updateMessage(ObjectId conversationId, Message updatedMessage) {
        return publishSaveEvent(updateQueries(null, conversationRepository.updateMessage(conversationId, updatedMessage)));
    }

    private Conversation publishSaveEvent(Conversation conversation) {
        Preconditions.checkNotNull(conversation, "Can't publish <null> conversation");
        eventPublisher.publishEvent(StoreServiceEvent.save(conversation.getId(), conversation.getMeta().getStatus(), this));
        return conversation;
    }
}
