package io.redlink.smarti.chatpal.repo;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.WriteResult;

import io.redlink.smarti.chatpal.model.ChatpalMessage;
import io.redlink.smarti.repositories.UpdatedIds;

public class ChatpalRepositoryImpl implements ChatpalRepositoryCustom {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final ProjectionOperation ID_MODIFIED_PROJECTION = Aggregation.project("id","modified");
    private static final GroupOperation GROUP_MODIFIED = Aggregation.group()
            .addToSet("id").as("ids")
            .max("modified").as("lastModified");

    
    private final MongoTemplate mongoTemplate;
    
    public ChatpalRepositoryImpl(MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }
    
    /* (non-Javadoc)
     * @see io.redlink.smarti.chatpal.repo.ChatpalRepositoryCustom#store(org.bson.types.ObjectId, java.util.Map)
     */
    @Override
    public void store(ObjectId client, Map<String,Object> chatpalMessage){
        assert chatpalMessage.get("id") != null;
        Object msgId = chatpalMessage.get("id");
        WriteResult result = mongoTemplate.upsert(
                Query.query(Criteria.where("msgId").is(msgId).and("client").is(client)),
                Update.update("data", chatpalMessage)
                    .set("removed", false) //
                    .currentDate("modified"),
                ChatpalMessage.class);
        if(log.isTraceEnabled()){
            if(result.isUpdateOfExisting()){
                log.trace("updated chatpal message {} for client {} [data: {}]", msgId, client, chatpalMessage);
            } else {
                log.trace("created chatpal message {} for client {} [data: {}]", msgId, client, chatpalMessage);
            }
        }
    }
    
    @Override
    public void markAsDeleted(ObjectId client, String msgId) {
        WriteResult result = mongoTemplate.updateMulti(
                Query.query(Criteria.where("msgId").is(msgId).and("client").is(client)),
                Update.update("removed", true).unset("data").currentDate("modified"),
                ChatpalMessage.class);
        if(log.isTraceEnabled()){
            if(result.getN() > 0){
                log.trace("marked chatpal message {} for client {} as deleted", msgId, client);
            } else {
                log.trace("no chatpal message {} for client {} present (no need to mark as deleted)", msgId, client);
            }
        }
        
    }
    @Override
    public void markAsDeleted(ObjectId client) {
        WriteResult result = mongoTemplate.updateMulti(
                Query.query(Criteria.where("client").is(client)),
                Update.update("removed", true).unset("data").currentDate("modified"),
                ChatpalMessage.class);
        if(log.isTraceEnabled()){
            if(result.getN() > 0){
                log.trace("marked {} chatpal messages for client {} as deleted", result.getN(), client);
            } else {
                log.trace("no chatpal messages for client {} present (no need to mark anything as deleted)", client);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see io.redlink.smarti.chatpal.repo.ChatpalRepositoryCustom#updatedSince(java.util.Date)
     */
    @Override
    public UpdatedIds<ObjectId> updatedSince(Date date) {
        //IMPLEMENTATION NOTES (Rupert Westenthaler, 2017-07-19):
        // * we need to use $gte as we might get additional updates in the same ms ...
        // * Instead of $max: modified we would like to use the current Server time of the
        //   aggregation, but this is not possible ATM (see https://jira.mongodb.org/browse/SERVER-23656)
        // * The use of $gte together with lastModified menas that we will repeat reporting the
        //   Entities updated at lastModified on every call. To avoid this a Workaround is used
        //   that increases the reported lastModified time by 1ms in cases no update was done
        //   since the last call (meaning the parsed date equals the lastModified)
        Aggregation agg = date != null ? //if a date is parsed search for updated after this date
                Aggregation.newAggregation(Aggregation.match(Criteria.where("modified").gte(date)),
                        ID_MODIFIED_PROJECTION, GROUP_MODIFIED) :
                    //else return all updates
                    Aggregation.newAggregation(ID_MODIFIED_PROJECTION, GROUP_MODIFIED);
        log.trace("UpdatedSince Aggregation: {}", agg);
        @SuppressWarnings("rawtypes")
        AggregationResults<UpdatedIds> aggResult = mongoTemplate.aggregate(agg, ChatpalMessage.class, UpdatedIds.class);
        if(log.isTraceEnabled()){
            log.trace("{} : {} updated since {}", ChatpalMessage.class, aggResult.getMappedResults(), 
                    date == null ? null : date.toInstant());
        }
        if(aggResult.getUniqueMappedResult() == null){
            return new UpdatedIds<>(date, Collections.emptyList());
        } else {
            UpdatedIds<ObjectId> updates = aggResult.getUniqueMappedResult();
            //NOTE: workaround for SERVER-23656 (see above impl. notes)
            if(date != null && date.equals(updates.getLastModified())) { //no update since the last request
                //increase the time by 1 ms to avoid re-indexing the last update
                return new UpdatedIds<>(new Date(date.getTime()+1), updates.ids());
            } else {
                return updates;
            }    
        }
    }
}
