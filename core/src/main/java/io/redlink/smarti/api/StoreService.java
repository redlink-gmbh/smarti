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

/**
 */
public abstract class StoreService implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher eventPublisher = null;

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

    public Conversation getConversationByChannelId(String channelId) {
        final ObjectId conversationId = mapChannelToConversationId(channelId);
        if (conversationId != null) {
            return get(conversationId);
        } else {
            final Conversation c = new Conversation();
            c.setChannelId(channelId);
            return store(c);
        }
    }

    public abstract ObjectId mapChannelToConversationId(String channelId);

    protected abstract Collection<ObjectId> listConversationIDsByHashedUser(String hashedUserId);

    public abstract List<String> listTagsByInfix(String query, int limit);

    public abstract List<String> listTagsByPrefix(String query, int limit);

    public abstract List<Pair<String, Long>> listTags(int rows, int offset);

    public abstract void deleteAll();

    public abstract long count();

    public abstract Conversation appendMessage(Conversation conversation, Message message);

}
