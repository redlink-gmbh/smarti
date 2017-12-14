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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;

import io.redlink.smarti.model.Analysis;

/**
 * Custom implementations of Analysis Repository
 *
 * @author RUpert Westenthaler
 */
public class AnalysisRepositoryImpl implements AnalysisRepositoryCustom {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final MongoTemplate mongoTemplate;

    public AnalysisRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Analysis updateAnalysis(Analysis analysis) {
        if(analysis == null){
            throw new NullPointerException();
        }
        final Query query = new Query();
        query.addCriteria(Criteria.where("conversation").is(analysis.getConversation()));
        query.addCriteria(Criteria.where("client").is(analysis.getClient()));
        //TODO: select the system generated analysis as soon as we support storing user modified 
        
        BasicDBObject data = new BasicDBObject();
        mongoTemplate.getConverter().write(analysis, data);
        if(data.remove("_id") != null){ //do not change the id
            log.warn("updateAnalysis call with Analysis having an ID. ID is expected to be NULL and will be ignored!");
        }
        final Update update = new Update();
        data.entrySet().stream().forEach(e -> update.set(e.getKey(), e.getValue()));

        final WriteResult writeResult = mongoTemplate.upsert(query, update, Analysis.class);
        if(writeResult.isUpdateOfExisting()){
            return mongoTemplate.findOne(query, Analysis.class);
        } else {
            return mongoTemplate.findById((ObjectId)writeResult.getUpsertedId(), Analysis.class);
        }
    }
}
