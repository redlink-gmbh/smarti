/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.repositories;

import com.google.common.collect.Lists;
import com.mongodb.DBObject;
import io.redlink.smarti.model.Conversation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Custom implementations of Conversation Repository
 *
 * @author Sergio Fernández
 */
public class ConversationRepositoryImpl implements ConversationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ConversationRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<String> findConversationIDs() {
        final Query query = new Query();
        query.fields().include("id");

        return Lists.transform(
                mongoTemplate.find(query, Conversation.class),
                Conversation::getId
        );
    }

    @Override
    public List<String> findConversationIDsByUser(String userId) {
        final Query query = new Query();
        query.addCriteria(Criteria.where("user.id").is(userId));
        query.fields().include("id");

        return Lists.transform(
                mongoTemplate.find(query, Conversation.class),
                Conversation::getId
        );
    }

    @Override
    public List<String> findTagsByPattern(Pattern pattern, int limit) {
        final Aggregation agg = newAggregation(
                project("meta.tags"),
                unwind("tags"),
                group("tags").count().as("count"),
                project("count").and("tags").previousOperation(),
                match(Criteria.where("tags").regex(pattern)),
                sort(Direction.ASC, "tags"),
                limit(limit));

        final AggregationResults<DBObject> results = mongoTemplate.aggregate(agg, Conversation.class, DBObject.class);
        return results.getMappedResults()
                .stream()
                .map(i -> (String) i.get("tags"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Pair<String, Long>> findTags(long limit) {
        return findTags(limit, 0);
    }

    @Override
    public List<Pair<String, Long>> findTags(long limit, long offset) {
        final Aggregation agg = newAggregation(
                project("meta.tags"),
                unwind("tags"),
                group("tags").count().as("count"),
                project("count").and("tags").previousOperation(),
                sort(Direction.DESC, "count"),
                skip(offset),
                limit(limit));

        final AggregationResults<DBObject> results = mongoTemplate.aggregate(agg, Conversation.class, DBObject.class);
        return results.getMappedResults()
                .stream()
                .map(i -> new ImmutablePair<>((String) i.get("tags"), (long) i.get("count")))
                .collect(Collectors.toList());
    }

}
