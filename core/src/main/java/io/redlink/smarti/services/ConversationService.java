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
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.repositories.AnalysisRepository;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private ApplicationEventPublisher eventPublisher;

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
     * @return the updated conversation as stored after the update and likely before processing has completed. Use the
     * <code>onCompleteCallback</code> to get the updated conversation including processing results
     */
    public Conversation update(Client client, Conversation conversation) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(client);
        if(!Objects.equal(client.getId(),conversation.getOwner())){
            throw new IllegalStateException("The parsed Client MUST BE the owner of the conversation!");
        }
        return storeService.store(conversation);
    }
    
    /**
     * Appends a message to the end of the conversation
     * @param onCompleteCallback called after the operation completes (including the optional asynchronous processing)
     * @param conversation the conversation (MUST BE owned by the parsed client)
     * @return the updated conversation as stored after the update and likely before processing has completed. Use the
     * <code>onCompleteCallback</code> to get the updated conversation including processing results
     */
    public Conversation appendMessage(Conversation conversation, Message message) {
        Preconditions.checkNotNull(conversation);
        Preconditions.checkNotNull(message);
        return storeService.appendMessage(conversation, message);
    }

    public Conversation completeConversation(Conversation conversation) {
        return storeService.completeConversation(conversation.getId());
    }

    public Conversation rateMessage(Conversation conversation, String messageId, int delta) {
        return storeService.adjustMessageVotes(conversation.getId(), messageId, delta);
    }


    public Conversation getConversation(Client client, ObjectId convId){
        return storeService.get(convId);
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

    public Page<Conversation> listConversations(Set<ObjectId> clientIDs, int page, int pageSize) {
        final PageRequest paging = new PageRequest(page, pageSize);
        if (CollectionUtils.isNotEmpty(clientIDs)) {
            return conversationRepository.findByOwnerIn(clientIDs, paging);
        } else {
            return conversationRepository.findAll(paging);
        }
    }

    public Page<Conversation> listConversations(ObjectId clientId, int page, int pageSize) {
        if (clientId == null) {
            return listConversations(Collections.emptySet(), page, pageSize);
        } else {
            return listConversations(Collections.singleton(clientId), page, pageSize);
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

    public boolean deleteMessage(ObjectId conversationId, String messageId) {
        final boolean success = conversationRepository.deleteMessage(conversationId, messageId);
        if (success) {
            final Conversation one = conversationRepository.findOne(conversationId);
            publishSaveEvent(one);
        }
        return success;
    }

    public Conversation updateMessage(ObjectId conversationId, Message updatedMessage) {
        Conversation con = publishSaveEvent(conversationRepository.updateMessage(conversationId, updatedMessage));
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
            analysisRepository.deleteByConversation(one.getId());
        }
        return one;
    }

    public Conversation updateConversationField(ObjectId conversationId, String field, Object data) {
        // TODO(westei): check whitelist of allowed fields
        Conversation con = publishSaveEvent(conversationRepository.updateConversationField(conversationId, field, data));
        // re-process updated conversation
        return con;
    }

    public Message getMessage(ObjectId conversationId, String messageId) {
        return conversationRepository.findMessage(conversationId, messageId);
    }

    public boolean exists(ObjectId conversationId, String messageId) {
        return conversationRepository.exists(conversationId, messageId);
    }
    /**
     * Updates a field of a message within the conversation
     * @param conversationId
     * @param messageId
     * @param field
     * @param data
     * @return The conversation on an update or <code>null</code> of no update was performed
     */
    public Conversation updateMessageField(ObjectId conversationId, String messageId, String field, Object data) {
        final Conversation con = conversationRepository.updateMessageField(conversationId, messageId, field, data);
        if (con != null) {
            publishSaveEvent(con);
        }
        return con;
    }
}
