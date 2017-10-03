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

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A MongoDB-based store for conversations
 *
 * @author Sergio Fernández
 */
@Component
@Primary
public class MongoDBStoreService extends StoreService {

    private final ConversationRepository conversationRepository;

    public MongoDBStoreService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    protected Conversation doStore(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    @Override
    public Conversation storeIfUnmodifiedSince(Conversation finalConversation, Date lastModified) {
        return conversationRepository.saveIfNotLastModifiedAfter(finalConversation, lastModified);
    }

    @Override
    public Collection<ObjectId> listConversationIDs() {
        return conversationRepository.findConversationIDs();
    }

    @Override
    public Conversation get(ObjectId conversationId) {
        return conversationRepository.findOne(conversationId);
    }

    @Override
    protected Collection<ObjectId> listConversationIDsByHashedUser(String hashedUserId) {
        return conversationRepository.findConversationIDsByUser(hashedUserId);
    }

    @Override
    protected Conversation doAppendMessage(Conversation conversation, Message message) {
        return conversationRepository.appendMessage(conversation, message);
    }

    @Override
    public ObjectId mapChannelToCurrentConversationId(String channelId) {
        return conversationRepository.findCurrentConversationIDByChannelID(channelId);
    }

    @Override
    protected Conversation doCompleteConversation(ObjectId conversationId) {
        return conversationRepository.completeConversation(conversationId);
    }

    @Override
    public Conversation adjustMessageVotes(ObjectId conversationId, String messageId, int delta) {
        return conversationRepository.adjustMessageVotes(conversationId, messageId, delta);
    }

    @Override
    public void deleteAll() {
        conversationRepository.deleteAll();
    }

    @Override
    public long count() {
        return conversationRepository.count();
    }

}
