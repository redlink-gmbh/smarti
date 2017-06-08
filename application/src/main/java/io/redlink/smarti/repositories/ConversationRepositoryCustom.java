/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.repositories;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Custom repository for Conversations
 *
 * @author Sergio Fernandez
 */
public interface ConversationRepositoryCustom {

    List<ObjectId> findConversationIDs();

    List<ObjectId> findConversationIDsByUser(String userId);

    List<String> findTagsByPattern(Pattern pattern, int limit);

    List<Pair<String, Long>> findTags(long limit);

    List<Pair<String, Long>> findTags(long limit, long offset);

    ObjectId findConversationIDByChannelID(String channelId);

    Conversation appendMessage(Conversation conversation, Message message);

    Conversation saveIfNotLastModifiedAfter(Conversation finalConversation, Date lastModified);
}
