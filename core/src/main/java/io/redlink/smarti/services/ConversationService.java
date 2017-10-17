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
import io.redlink.smarti.repositories.AnalysisRepository;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private QueryBuilderService queryBuilderService;
    
    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ConfigurationService confService;

    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private AnalysisRepository analysisRepository;

    public ConversationService() {
    }
    
    /**
     * Updates/Stores the parsed conversation
     * @param client the client
     * @param conversation the conversation (MUST BE owned by the parsed client)
     * @param process if the conversation needs to be processed (done asynchronously)
     * @return the updated conversation as stored after the update and likely before processing has completed. Use the
     * <code>onCompleteCallback</code> to get the updated conversation including processing results
     */
    public Conversation update(Client client, Conversation conversation, boolean process) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(client);
        if(!Objects.equal(client.getId(),conversation.getOwner())){
            throw new IllegalStateException("The parsed Client MUST BE the owner of the conversation!");
        }
        
        final Conversation storedConversation = storeService.store(conversation);
        if(process){
            process(client, storedConversation);
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
    public Conversation appendMessage(Client client, Conversation conversation, Message message, boolean process) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(client);
        if(!Objects.equal(client.getId(),conversation.getOwner())){
            throw new IllegalStateException("The parsed Client MUST BE the owner of the conversation!");
        }

        final Conversation storedConversation = storeService.appendMessage(conversation, message);

        if (process) {
            process(client, storedConversation);
        }

        return storedConversation;
    }

    /**
     * Processes the conversation for the parsed client and saved the processing results if the parsed client is also
     * the owner of the conversation AND the conversation was not updated in the meantime. 
     * @param client
     * @param conversation
     * @param onCompleteCallback
     */
    private Analysis getOrProcessAnalysis(Client client, final Conversation conversation) {
        Analysis analysis = null;
        //the last modified date is either the date when the conversation or the configuration was changed last
        Date date = conversation.getLastModified();
        Configuration config = getConfig(client, conversation);
        Date confDate = config.getModified();
        if(confDate != null && confDate.after(date)){
            date = confDate;
        }
        if(client != null && Objects.equal(client.getId(), conversation.getOwner())){
            analysis = analysisRepository.findByConversationAndDate(conversation.getId(), date);
        } //for now we only cache analysis from the conversation owner
        
        if(analysis == null){
            analysis = process(client, conversation);
        }
        return analysis;
    }

    private Analysis process(Client client, final Conversation conversation) {
        Analysis analysis;
        analysis = prepareService.prepare(client, conversation);
   
        templateService.updateTemplates(client, conversation, analysis);
        
        queryBuilderService.buildQueries(client, conversation, analysis);
   
        if(client != null && Objects.equal(conversation.getOwner(), client.getId())){
            analysis = analysisRepository.updateAnalysis(analysis);
   
            if (log.isDebugEnabled()) {
                logConversation(conversation, analysis);
            }
   
            eventPublisher.publishEvent(new ConversationProcessCompleteEvent(conversation, analysis));
        } //else we do not cache analysis results for clients different as the owner of the conversation
        return analysis;
    }

    private Configuration getConfig(Client client, final Conversation conversation) {
        Configuration config;
        if(client == null){
            config = confService.getClientConfiguration(conversation.getOwner());
        } else {
            config = confService.getClientConfiguration(client);
        }
        if(config == null){
            log.trace("Client {} does not have a configuration. Will use default configuration", client);
            config = confService.getDefaultConfiguration();
        }
        return config;
    }

    private void logConversation(Conversation c, Analysis a) {
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
        if(a != null){
            if(a.getTokens() != null){
                log.debug(" > {} tokens:", a.getTokens().size());
                AtomicInteger count = new AtomicInteger(0);
                a.getTokens().forEach(t -> {
                    log.debug("    {}. {}",count.getAndIncrement(), t);
                });
            }
            if(a.getTemplates() != null){
                log.debug(" > {} templates:", a.getTemplates().size());
                AtomicInteger count = new AtomicInteger(0);
                a.getTemplates().forEach(t -> {
                    log.debug("    {}. {}", count.getAndIncrement(), t);
                    if (CollectionUtils.isNotEmpty(t.getQueries())) {
                        log.debug("    > with {} queries", t.getQueries().size());
                        t.getQueries().forEach(q -> log.debug("       - {}", q));
                    }
                });
            }
        }
    }

    public Conversation appendMessage(Client client, Conversation conversation, Message message) {
        return appendMessage(client, conversation, message, true);
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
        return storeService.get(convId);
    }
    
    public Analysis getAnalysis(Client client, Conversation conversation){
        return getOrProcessAnalysis(client, conversation);
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
        return publishSaveEvent(conversationRepository.updateConversationStatus(conversationId, newStatus));
    }

    public boolean deleteMessage(ObjectId conversationId, String messageId, boolean process) {
        final boolean success = conversationRepository.deleteMessage(conversationId, messageId);
        if (success) {
            final Conversation one = conversationRepository.findOne(conversationId);
            publishSaveEvent(one);
            if(process){
                process(null, one);
            }
        }
        return success;
    }

    public Conversation updateMessage(ObjectId conversationId, Message updatedMessage, boolean process) {
        // TODO: also re-analyze!
        Conversation con = publishSaveEvent(conversationRepository.updateMessage(conversationId, updatedMessage));
        if(process){
            process(null, con);
        }
        return con;
    }

    private Conversation publishSaveEvent(Conversation conversation) {
        Preconditions.checkNotNull(conversation, "Can't publish <null> conversation");
        eventPublisher.publishEvent(StoreServiceEvent.save(conversation.getId(), conversation.getMeta().getStatus(), this));
        return conversation;
    }

    public Conversation deleteConversation(ObjectId conversationId) {
        final Conversation one = conversationRepository.findOne(conversationId);
        if (one != null) {
            conversationRepository.delete(conversationId);
            eventPublisher.publishEvent(StoreServiceEvent.delete(conversationId, this));
        }
        return one;
    }

    public Conversation updateConversationField(ObjectId conversationId, String field, Object data, boolean process) {
        // TODO(westei): check whitelist of allowed fields
        Conversation con = publishSaveEvent(conversationRepository.updateConversationField(conversationId, field, data));
        // re-process updated conversation
        if(process){
            process(null, con);
        }
        return con;
    }

    public Message getMessage(ObjectId conversationId, String messageId) {
        return conversationRepository.findMessage(conversationId, messageId);
    }

    public boolean exists(ObjectId conversationId, String messageId) {
        return conversationRepository.exists(conversationId, messageId);
    }

    public Message updateMessageField(ObjectId conversationId, String messageId, String field, Object data, boolean process) {
        final Conversation con = conversationRepository.updateMessageField(conversationId, messageId, field, data);
        if (con != null) {
            publishSaveEvent(con);
            if(process){
                process(null, con);
            }
        }
        return con.getMessages().stream()
                .filter(m -> messageId.equals(m.getId()))
                .findFirst().orElse(null);
    }
}
