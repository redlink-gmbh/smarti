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

package io.redlink.smarti.repositories;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.*;
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

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final MongoTemplate mongoTemplate;

    public ConversationRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;

        /* see #findLegacyConversation */
        mongoTemplate.indexOps(Conversation.class)
                .ensureIndex(new Index()
                        .named("legacyLookup")
                        .on("owner", Direction.ASC)
                        .on("meta.properties.channel_id", Direction.ASC)
                        .on("context.contextType", Direction.ASC)
                        .sparse()
                );
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
        final Query isMessageEdit = new Query(Criteria.where("_id").is(conversation.getId()))
                .addCriteria(Criteria.where("messages._id").is(message.getId()));

        final Query query;
        final Update update;
        if (mongoTemplate.exists(isMessageEdit, Conversation.class)) {
            return updateMessage(conversation.getId(), message);
        } else {
            query = new Query();
            query.addCriteria(Criteria.where("_id").is(conversation.getId()));
            query.addCriteria(Criteria.where("messages").size(conversation.getMessages().size()));

            update = new Update();
            update.addToSet("messages", message)
                    .currentDate("lastModified");
        }

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(conversation.getId(), Conversation.class);
        } else {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public Conversation updateMessage(ObjectId conversationId, Message message) {
        final Query query = new Query(Criteria.where("_id").is(conversationId))
                .addCriteria(Criteria.where("messages._id").is(message.getId()));

        final Update update = new Update()
                .set("messages.$", message)
                .currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(conversationId, Conversation.class);
        } else {
            return null;
        }
    }

    @Override
    public Conversation saveIfNotLastModifiedAfter(Conversation conversation, Date lastModified) {
        
        final Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(conversation.getId()));
        query.addCriteria(Criteria.where("lastModified").lte(lastModified));

        BasicDBObject data = new BasicDBObject();
        mongoTemplate.getConverter().write(conversation, data);
        final Update update = new Update();
        data.entrySet().stream()
            .filter(e -> !Objects.equals("lastModified", e.getKey()))
            .forEach(e -> update.set(e.getKey(), e.getValue()));
        update.currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(conversation.getId(), Conversation.class);
        } else {
            throw new ConcurrentModificationException(
                    String.format("Conversation %s has been modified after %tF_%<tT.%<tS (%tF_%<tT.%<tS)", conversation.getId(), lastModified, conversation.getLastModified()));
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
    public ObjectId findCurrentConversationIDByChannelID(String channelId) {
        final Query query = new Query();
        query.addCriteria(where("channelId").is(channelId))
                .addCriteria(where("meta.status").ne(ConversationMeta.Status.Complete));
        query.fields().include("id");
        query.with(new Sort(Direction.DESC, "lastModified"));

        final Conversation one = mongoTemplate.findOne(query, Conversation.class);
        if (one == null) {
            return null;
        } else {
            return one.getId();
        }
    }

    @Override
    public Conversation adjustMessageVotes(ObjectId conversationId, String messageId, int delta) {
        final Query query = new Query(Criteria.where("_id").is(conversationId))
                .addCriteria(Criteria.where("messages._id").is(messageId));
        final Update update = new Update()
                .inc("messages.$.votes", delta)
                .currentDate("lastModified");

        mongoTemplate.updateFirst(query, update, Conversation.class);

        return mongoTemplate.findById(conversationId, Conversation.class);
    }

    @Override
    public Conversation updateConversationStatus(ObjectId conversationId, ConversationMeta.Status status) {
        return updateConversationField(conversationId, "meta.status", status);
    }

    @Override
    public Conversation updateConversationField(ObjectId conversationId, String field, Object data) {
        final Query query = new Query(Criteria.where("_id").is(conversationId));
        final Update update = new Update()
                .set(field, data)
                .currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() < 1) return null;

        return mongoTemplate.findById(conversationId, Conversation.class);
    }
    
    @Override
    public Conversation deleteConversationField(ObjectId conversationId, String field) {
        final Query query = new Query(Criteria.where("_id").is(conversationId));
        final Update update = new Update()
                .unset(field)
                .currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() < 1) return null;

        return mongoTemplate.findById(conversationId, Conversation.class);
    }

    @Override
    public boolean deleteMessage(ObjectId conversationId, String messageId) {
        final Query query = new Query(Criteria.where("_id").is(conversationId));
        final Update update = new Update()
                .pull("messages", new BasicDBObject("_id", messageId))
                .currentDate("lastModified");

        final WriteResult result = mongoTemplate.updateFirst(query, update, Conversation.class);
        return result.getN() == 1;
    }

    @Override
    public Conversation updateMessageField(ObjectId conversationId, String messageId, String field, Object data) {
        final Query query = new Query(Criteria.where("_id").is(conversationId))
                .addCriteria(Criteria.where("messages._id").is(messageId));
        final Update update = new Update()
                .set("messages.$." + field, data)
                .currentDate("lastModified");

        final WriteResult writeResult = mongoTemplate.updateFirst(query, update, Conversation.class);
        if (writeResult.getN() < 1) return null;

        return mongoTemplate.findById(conversationId, Conversation.class);
    }

    @Override
    public Message findMessage(ObjectId conversationId, String messageId) {
        // TODO: with mongo 3.4 you could do this with aggregation
        /*
        final TypedAggregation aggregation = newAggregation(Conversation.class,
                Aggregation.match(Criteria.where("_id").is(conversationId)),
                Aggregation.project("messages"),
                Aggregation.unwind("messages"),
                Aggregation.replaceRoot("messages"),
                Aggregation.match(Criteria.where("_id").is(messageId))
        );
        return mongoTemplate.aggregate(aggregation, Message.class).getUniqueMappedResult();
        */

        final Conversation conversation = mongoTemplate.findById(conversationId, Conversation.class);
        if (conversation == null) {
            return null;
        } else {
            return conversation.getMessages().stream()
                    .filter(m -> messageId.equals(m.getId()))
                    .findFirst().orElse(null);
        }
    }

    @Override
    public boolean exists(ObjectId conversationId, String messageId) {
        final Query query = Query.query(Criteria
                .where("_id").is(conversationId)
                .and("messages._id").is(messageId));
        return mongoTemplate.exists(query, Conversation.class);
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
    
    private static final ProjectionOperation ID_MODIFIED_PROJECTION = Aggregation.project("id","lastModified");
    private static final GroupOperation GROUP_MODIFIED = Aggregation.group()
            .addToSet("id").as("ids")
            .max("lastModified").as("lastModified");
    
    @Override
    public UpdatedIds<ObjectId> updatedSince(Date date, long limit) {
        //IMPLEMENTATION NOTES (Rupert Westenthaler, 2017-07-19):
        // * we need to use $gte as we might get additional updates in the same ms ...
        // * Instead of $max: modified we would like to use the current Server time of the
        //   aggregation, but this is not possible ATM (see https://jira.mongodb.org/browse/SERVER-23656)
        // * The use of $gte together with lastModified menas that we will repeat reporting the
        //   Entities updated at lastModified on every call. To avoid this a Workaround is used
        //   that increases the reported lastModified time by 1ms in cases no update was done
        //   since the last call (meaning the parsed date equals the lastModified)
        AggregationResults<UpdatedIds> aggResult = mongoTemplate.aggregate(getUpdatedSinceAggregation(date, limit),Conversation.class, 
                UpdatedIds.class);
        if(log.isTraceEnabled()){
            log.trace("updated Conversations : {}", aggResult.getMappedResults());
        }
        if(aggResult.getUniqueMappedResult() == null){
            return new UpdatedIds<ObjectId>(date, Collections.emptyList());
        } else {
            UpdatedIds<ObjectId> updates = aggResult.getUniqueMappedResult();
            //NOTE: workaround for SERVER-23656 (see above impl. notes)
            if(date != null && date.equals(updates.getLastModified())) { //no update since the last request
                //increase the time by 1 ms to avoid re-indexing the last update
                return new UpdatedIds<ObjectId>(new Date(date.getTime()+1), updates.ids());
            } else {
                return updates;
            }    
        }
    }
    
    private Aggregation getUpdatedSinceAggregation(Date since, long limit) {
        List<AggregationOperation> ops = new LinkedList<>();
        if(since != null){
            ops.add(Aggregation.match(Criteria.where("lastModified").gte(since)));
        }
        ops.add(Aggregation.sort(Direction.ASC,"lastModified"));
        if(limit > 0){
            ops.add(Aggregation.limit(limit));
        }
        ops.add(ID_MODIFIED_PROJECTION);
        ops.add(GROUP_MODIFIED);
        Aggregation agg = Aggregation.newAggregation(ops);
        log.trace("UpdatedSince Aggregation: {}", agg);
        return agg;
    }

    @Override
    public Conversation findLegacyConversation(ObjectId ownerId, String contextType, String channelId) {
        Query query = new Query();
        query.addCriteria(where("owner").is(ownerId));
        query.addCriteria(where("meta.properties.channel_id").is(channelId));
        query.addCriteria(where("context.contextType").is(contextType));

        return mongoTemplate.findOne(query, Conversation.class);
    }
}
