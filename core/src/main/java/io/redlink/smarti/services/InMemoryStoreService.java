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
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static io.redlink.smarti.model.ConversationMeta.PROP_TAGS;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An in-memory store for conversations.
 */
@Component("inMemoryStoreService")
@Profile("embedded")
public class InMemoryStoreService extends StoreService {

    private final Map<ObjectId, Conversation> storage = new ConcurrentHashMap<>();

    @Override
    protected Conversation doStore(Conversation conversation) {
        if(conversation.getId() == null) conversation.setId(new ObjectId());
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
    protected Conversation doAppendMessage(Conversation conversation, Message message) {
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
    protected Conversation doCompleteConversation(ObjectId conversationId) {
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
