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
import com.mongodb.WriteResult;
import io.redlink.smarti.model.SmartiUser;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UserRepositoryImpl implements UserRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public UserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SmartiUser create(SmartiUser user) {
        try {
            mongoTemplate.insert(user);
            return mongoTemplate.findById(user.getLogin(), SmartiUser.class);
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    @Override
    public SmartiUser removeClient(String username, ObjectId id) {
        return mongoTemplate.findAndModify(
                byLogin(username),
                new Update().pull(SmartiUser.FIELD_CLIENTS, id),
                SmartiUser.class);
    }

    @Override
    public SmartiUser addClient(String username, ObjectId id) {
        return mongoTemplate.findAndModify(
                byLogin(username),
                new Update().addToSet(SmartiUser.FIELD_CLIENTS, id),
                SmartiUser.class
        );
    }

    @Override
    public List<SmartiUser> findAllWithFilter(String filter) {
        final Query query = new Query();
        if (StringUtils.isNotBlank(filter)) {
            final Pattern p = Pattern.compile("^" + Pattern.quote(filter) + ".*");
            query.addCriteria(new Criteria()
                            .orOperator(
                                    Criteria.where("_id").regex(p),
                                    Criteria.where(SmartiUser.PROFILE_FIELD(SmartiUser.ATTR_EMAIL)).regex(p),
                                    Criteria.where(SmartiUser.PROFILE_FIELD(SmartiUser.ATTR_EMAIL)).regex(p)
                            )
            );
        }
        return mongoTemplate.find(query, SmartiUser.class);
    }

    @Override
    public SmartiUser updateProfile(String login, Map<String, String> profile) {
        final WriteResult writeResult = mongoTemplate.updateFirst(byLogin(login), Update.update(SmartiUser.FIELD_PROFILE, profile), SmartiUser.class);
        if (writeResult.getN() == 1) {
            return mongoTemplate.findById(login, SmartiUser.class);
        }
        return null;
    }

    private static Query byLogin(String login) {
        return new Query(Criteria.where("_id").is(login));
    }
}
