/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.repositories;

import com.google.common.collect.Lists;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

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
    public List<ObjectId> findConversationIDs() {
        final Query query = new Query();
        query.fields().include("id");

        return Lists.transform(
                mongoTemplate.find(query, Conversation.class),
                Conversation::getId
        );
    }

    @Override
    public Conversation appendMessage(Conversation conversation, Message message) {

        final Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(conversation.getId()));
        query.addCriteria(Criteria.where("messages").size(conversation.getMessages().size()));

        final Update update = new Update();
        update.addToSet("messages", message)
                .currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(conversation.getId(), Conversation.class);
        } else {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public Conversation saveIfNotLastModifiedAfter(Conversation conversation, Date lastModified) {
        final Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(conversation.getId()));
        query.addCriteria(Criteria.where("lastModified").lte(lastModified));

        final Update update = new Update();
        update.set("channelId", conversation.getChannelId())
                .set("meta", conversation.getMeta())
                .set("user", conversation.getUser())
                .set("messages", conversation.getMessages())
                .set("tokens", conversation.getTokens())
                .set("queryTemplates", conversation.getQueryTemplates())
                .set("context", conversation.getContext())
                ;
        update.currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(conversation.getId(), Conversation.class);
        } else {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public List<ObjectId> findConversationIDsByUser(String userId) {
        final Query query = new Query();
        query.addCriteria(where("user.id").is(userId));
        query.fields().include("id");

        return Lists.transform(
                mongoTemplate.find(query, Conversation.class),
                Conversation::getId
        );
    }

    @Override
    public ObjectId findConversationIDByChannelID(String channelId) {
        final Query query = new Query();
        query.addCriteria(where("channelId").is(channelId))
                .addCriteria(where("meta.state").ne(ConversationMeta.Status.Complete));
        query.fields().include("id");

        final Conversation one = mongoTemplate.findOne(query, Conversation.class);
        if (one == null) {
            return null;
        } else {
            return one.getId();
        }
    }

    @Override
    public List<String> findTagsByPattern(Pattern pattern, int limit) {
        final Aggregation agg = newAggregation(
                project("meta.tags"),
                unwind("tags"),
                group("tags").count().as("count"),
                project("count").and("tags").previousOperation(),
                match(where("tags").regex(pattern)),
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
