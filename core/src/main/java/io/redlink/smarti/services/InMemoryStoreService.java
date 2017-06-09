/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An in-memory store for conversations.
 */
@Component("inMemoryStoreService")
@ConditionalOnMissingBean(StoreService.class)
public class InMemoryStoreService extends StoreService {

    private final Map<ObjectId, Conversation> storage = new ConcurrentHashMap<>();

    @Override
    protected Conversation doStore(Conversation conversation) {
        Preconditions.checkNotNull(conversation.getId());
        storage.put(conversation.getId(), conversation);
        return conversation;
    }

    @Override
    public Conversation storeIfUnmodifiedSince(Conversation conversation, Date lastModified) {
        Preconditions.checkNotNull(conversation);
        if (lastModified == null) {
            return this.store(conversation);
        } else {
            final Conversation cc = get(conversation.getId());
            final Date ccLastModified = cc.getLastModified();
            if (ccLastModified != null &&
                    (ccLastModified.equals(lastModified) || ccLastModified.before(lastModified))) {
                return this.store(conversation);
            }

            return cc;
        }
    }

    @Override
    public Collection<ObjectId> listConversationIDs() {
        return storage.keySet();
    }

    @Override
    protected Collection<ObjectId> listConversationIDsByHashedUser(String hashedUserId) {
        return storage.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getValue().getUser() != null)
                .filter(e -> StringUtils.equals(hashedUserId, e.getValue().getUser().getId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Conversation appendMessage(Conversation conversation, Message message) {
        final Conversation cc = get(conversation.getId());
        int pos = 0;
        do {
            if (StringUtils.equals(cc.getMessages().get(pos).getId(), message.getId())) {
                break;
            }
            pos++;
        } while (pos < cc.getMessages().size());
        if (pos < cc.getMessages().size()) {
            cc.getMessages().remove(pos);
        }
        cc.getMessages().add(pos, message);
        cc.setLastModified(new Date());
        return cc;
    }

    @Override
    public Conversation completeConversation(ObjectId conversationId) {
        final Conversation conversation = storage.get(conversationId);
        if (conversation != null) {
            conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
        }
        return conversation;
    }

    @Override
    public Conversation adjustMessageVotes(ObjectId id, String messageId, int delta) {
        final Conversation conversation = storage.get(id);;
        if (conversation != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (conversation) {
                conversation.getMessages().stream()
                        .filter(m -> StringUtils.equals(m.getId(), messageId))
                        .forEach(m -> m.setVotes(m.getVotes() + delta));
            }
        }
        return conversation;
    }

    @Override
    public Conversation get(ObjectId conversationId) {
        return storage.get(conversationId);
    }

    @Override
    public List<String> listTagsByInfix(String query, int limit) {
        return listTagsBy(t -> StringUtils.containsIgnoreCase(t, query), limit);
    }

    @Override
    public List<String> listTagsByPrefix(String query, int limit) {
        return listTagsBy(t -> StringUtils.startsWithIgnoreCase(t, query), limit);
    }

    protected List<String> listTagsBy(Predicate<String> predicate, int limit) {
        return storage.values().stream()
                .filter(Objects::nonNull)
                .map(Conversation::getMeta)
                .map(ConversationMeta::getTags)
                .flatMap(Collection::stream)
                .filter(predicate)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<Pair<String, Long>> listTags(int rows, int offset) {
        return storage.values().stream()
                .filter(Objects::nonNull)
                .map(Conversation::getMeta)
                .map(ConversationMeta::getTags)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .skip(offset)
                .limit(rows)
                .map(e -> ImmutablePair.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public ObjectId mapChannelToCurrentConversationId(String channelId) {
        return storage.values().stream()
                .filter(c -> Objects.equals(c.getChannelId(), channelId))
                .filter(c -> c.getMeta().getStatus() != ConversationMeta.Status.Complete)
                .sorted((a, b) -> a.getMeta().getStatus().compareTo(b.getMeta().getStatus()))
                .map(Conversation::getId)
                .findFirst().orElse(null);
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }

    @Override
    public long count() {
        return storage.size();
    }

}
