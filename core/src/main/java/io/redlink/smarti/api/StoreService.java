/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.api;

import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 */
public abstract class StoreService implements ApplicationEventPublisherAware {

    protected ApplicationEventPublisher eventPublisher = null;

    public final Conversation store(Conversation conversation) {
        conversation.setLastModified(new Date());
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
