/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.services;

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.repositories.ConversationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
    public Collection<String> listConversationIDs() {
        return conversationRepository.findConversationIDs();
    }

    @Override
    public Conversation get(String conversationId) {
        return conversationRepository.findOne(conversationId);
    }

    @Override
    protected Collection<String> listConversationIDsByHashedUser(String hashedUserId) {
        return conversationRepository.findConversationIDsByUser(hashedUserId);
    }

    @Override
    public Conversation appendMessage(Conversation conversation, Message message) {
        return conversationRepository.appendMessage(conversation, message);
    }

    @Override
    public String mapChannelToConversationId(String channelId) {
        return conversationRepository.findConversationIDByChannelID(channelId);
    }

    @Override
    public List<String> listTagsByInfix(String query, int limit) {
        final Pattern pattern = Pattern.compile(String.format(".*%s.*", Pattern.quote(query)));
        return conversationRepository.findTagsByPattern(pattern, limit);
    }

    @Override
    public List<String> listTagsByPrefix(String query, int limit) {
        final Pattern pattern = Pattern.compile(String.format("^%s.*", Pattern.quote(query)));
        return conversationRepository.findTagsByPattern(pattern, limit);
    }

    @Override
    public List<Pair<String, Long>> listTags(int rows, int offset) {
        return conversationRepository.findTags(rows, offset);
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
