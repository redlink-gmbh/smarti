/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.repositories;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Custom repository for Conversations
 *
 * @author Sergio Fernandez
 */
public interface ConversationRepositoryCustom {

    List<String> findConversationIDs();

    List<String> findConversationIDsByUser(String userId);

    List<String> findTagsByPattern(Pattern pattern, int limit);

    List<Pair<String, Long>> findTags(long limit);

    List<Pair<String, Long>> findTags(long limit, long offset);

}
