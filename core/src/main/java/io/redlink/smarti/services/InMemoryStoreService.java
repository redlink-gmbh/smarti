/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.services;

import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An in-memory store for conversations.
 */
@Component("inMemoryStoreService")
@Profile("default")
public class InMemoryStoreService extends StoreService {

    private final Map<String, Conversation> storage = new ConcurrentHashMap<>();

    @Override
    protected Conversation doStore(Conversation conversation) {
        Preconditions.checkNotNull(conversation.getId());
        storage.put(conversation.getId(), conversation);
        return conversation;
    }

    @Override
    public Collection<String> listConversationIDs() {
        return storage.keySet();
    }

    @Override
    protected Collection<String> listConversationIDsByHashedUser(String hashedUserId) {
        return storage.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getValue().getUser() != null)
                .filter(e -> StringUtils.equals(hashedUserId, e.getValue().getUser().getId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Conversation get(String conversationId) {
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
    public void deleteAll() {
        storage.clear();
    }

    @Override
    public long count() {
        return storage.size();
    }

}
