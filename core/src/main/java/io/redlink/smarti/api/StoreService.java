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

package io.redlink.smarti.api;

import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.exception.ConflictException;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 */
public abstract class StoreService implements ApplicationEventPublisherAware {

    protected ApplicationEventPublisher eventPublisher = null;

    public final Conversation store(Conversation conversation) {
        conversation.setLastModified(new Date());
        if(conversation.getId() != null){ //if we update an existing we need to validate the clientId value
            Conversation persisted = get(conversation.getId());
            if(persisted == null){
                throw new NotFoundException(Conversation.class, conversation.getId());
            } else {
                if(conversation.getClientId() == null){
                    conversation.setClientId(persisted.getClientId());
                } else if(!Objects.equals(conversation.getClientId(), persisted.getClientId())){
                    throw new ConflictException(Conversation.class, "clientId", "The clientId MUST NOT be changed for an existing conversation!");
                }
            }
        } else { //create a new conversation
            if(StringUtils.isBlank(conversation.getClientId())){
                throw new IllegalStateException("The clientId MUST NOT be NULL nor empty for a new conversation!");
            }
        }
        final Conversation stored = doStore(conversation);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(StoreServiceEvent.save(conversation.getId(), conversation.getMeta().getStatus(), this));
        }
        return stored;
    }

    public abstract Conversation storeIfUnmodifiedSince(Conversation finalConversation, Date lastModified);

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        eventPublisher = applicationEventPublisher;
    }

    protected abstract Conversation doStore(Conversation conversation);

    public abstract Collection<ObjectId> listConversationIDs();

    public abstract Conversation get(ObjectId conversationId);

    public final Collection<ObjectId> listConversationIDsByUser(String userId) {
        return listConversationIDsByHashedUser(userId);
    }

    public Conversation getCurrentConversationByChannelId(String channelId) {
        return getCurrentConversationByChannelId(channelId, Conversation::new);
    }

    public Conversation getCurrentConversationByChannelId(String channelId, Supplier<Conversation> supplier) {
        final ObjectId conversationId = mapChannelToCurrentConversationId(channelId);
        if (conversationId != null) {
            return get(conversationId);
        } else {
            final Conversation c = supplier.get();
            if(c != null){
                c.setId(null);
                c.setChannelId(channelId);
                return store(c);
            } else {
                return null;
            }
        }
    }

    public abstract ObjectId mapChannelToCurrentConversationId(String channelId);

    protected abstract Collection<ObjectId> listConversationIDsByHashedUser(String hashedUserId);

    public abstract List<String> listTagsByInfix(String query, int limit);

    public abstract List<String> listTagsByPrefix(String query, int limit);

    public abstract List<Pair<String, Long>> listTags(int rows, int offset);

    public abstract void deleteAll();

    public abstract long count();

    public final Conversation appendMessage(Conversation conversation, Message message) {
        final Conversation stored = doAppendMessage(conversation, message);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(StoreServiceEvent.save(stored.getId(), stored.getMeta().getStatus(), this));
        }
        return stored;
    }

    protected abstract Conversation doAppendMessage(Conversation conversation, Message message);

    public final Conversation completeConversation(ObjectId conversationId) {
        final Conversation stored = doCompleteConversation(conversationId);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(StoreServiceEvent.save(stored.getId(), stored.getMeta().getStatus(), this));
        }
        return stored;
    }

    protected abstract Conversation doCompleteConversation(ObjectId conversationId);

    public abstract Conversation adjustMessageVotes(ObjectId id, String messageId, int delta);
}
