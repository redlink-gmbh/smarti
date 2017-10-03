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
import io.redlink.smarti.services.ConversationService;

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
 * Low level service that allows to have different storage implementations for {@link Conversation}. Higher level
 * components and web-services will want to use the {@link ConversationService} instead
 * @see ConversationService
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
                if(conversation.getOwner() == null){
                    conversation.setOwner(persisted.getOwner());
                } else if(!Objects.equals(conversation.getOwner(), persisted.getOwner())){
                    throw new ConflictException(Conversation.class, "clientId", "The clientId MUST NOT be changed for an existing conversation!");
                }
            }
        } else { //create a new conversation
            if(conversation.getOwner() == null){
                throw new ConflictException(Conversation.class, "owner", "The owner MUST NOT be NULL nor empty for a new conversation!");
            }
        }
        final Conversation stored = doStore(conversation);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(StoreServiceEvent.save(conversation.getId(), conversation.getMeta().getStatus(), this));
        }
        return stored;
    }
    /**
     * Stores a conversation if it was not updated since the <code>lastModified</code> date. Used to store
     * processing results for a conversation. Those results are only stored if the conversation has not changed in the
     * meantime.
     * @param conversation the conversation to store
     * @param lastModified the last modified date
     * @return the conversation as stored
     */
    public abstract Conversation storeIfUnmodifiedSince(Conversation conversation, Date lastModified);

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        eventPublisher = applicationEventPublisher;
    }

    /**
     * Stores the parsed conversation. This is called by {@link #store(Conversation)} after all required validity checks
     * @param conversation the conversation to store
     * @return the conversation as stored
     */
    protected abstract Conversation doStore(Conversation conversation);

    public abstract Collection<ObjectId> listConversationIDs();

    public abstract Conversation get(ObjectId conversationId);

    public final Collection<ObjectId> listConversationIDsByUser(String userId) {
        return listConversationIDsByHashedUser(userId);
    }

    public abstract ObjectId mapChannelToCurrentConversationId(String channelId);

    protected abstract Collection<ObjectId> listConversationIDsByHashedUser(String hashedUserId);

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
