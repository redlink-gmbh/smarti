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

import com.mongodb.DuplicateKeyException;
import io.redlink.smarti.model.SmartiUser;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class UserRepositoryImpl implements UserRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public UserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SmartiUser create(SmartiUser user) {
        try {
            mongoTemplate.insert(user);
            return mongoTemplate.findById(user.getUsername(), SmartiUser.class);
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    @Override
    public SmartiUser removeClient(String username, ObjectId id) {
        return mongoTemplate.findAndModify(
                byUsername(username),
                new Update().pull(SmartiUser.FIELD_CLIENTS, id),
                SmartiUser.class);
    }

    @Override
    public SmartiUser addClient(String username, ObjectId id) {
        return mongoTemplate.findAndModify(
                byUsername(username),
                new Update().addToSet(SmartiUser.FIELD_CLIENTS, id),
                SmartiUser.class
        );
    }

    private Query byUsername(String username) {
        return new Query(Criteria.where("_id").is(username));
    }
}
