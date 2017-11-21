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
package io.redlink.smarti.auth.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Collections2;
import com.mongodb.WriteResult;
import io.redlink.smarti.auth.AttributedUserDetails;
import io.redlink.smarti.auth.SecurityConfigurationProperties;
import io.redlink.utils.HashUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 */
@Service
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
public class MongoUserDetailsService implements UserDetailsService {
    static final String FIELD_RECOVERY = "recovery";
    static final String FIELD_PASSWORD = "password";
    static final String FIELD_ROLES = "roles";
    static final String FIELD_TOKEN = "token";
    static final String FIELD_EXPIRES = "expires";

    private Logger log = LoggerFactory.getLogger(MongoUserDetailsService.class);

    private final MongoTemplate mongoTemplate;

    private final SecurityConfigurationProperties securityConfig;
    private final MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder;

    public MongoUserDetailsService(MongoTemplate mongoTemplate, SecurityConfigurationProperties securityConfig, MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.securityConfig = securityConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    protected void initialize() {
        // Check for at least one admin-user
        final long adminUsers;
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            adminUsers = mongoTemplate.count(Query.query(Criteria.where(FIELD_ROLES).is("ADMIN")), MongoUser.class, securityConfig.getMongo().getCollection());
        } else {
            adminUsers = mongoTemplate.count(Query.query(Criteria.where(FIELD_ROLES).is("ADMIN")), MongoUser.class);
        }
        if (adminUsers < 1) {
            log.debug("No admin-users found, creating");
            String adminUser = "admin";
            int i = 0;
            final String password = UUID.randomUUID().toString();
            final String encodedPassword = passwordEncoder.encodePassword(password);
            while (!createAdminUser(adminUser, encodedPassword)) {
                adminUser = String.format("admin%d", ++i);
            }
            log.error("Created new Admin-User '{}' with password '{}'", adminUser, password);
        } else {
            log.debug("Found {} Admin-Users", adminUsers);
        }
    }

    private boolean createAdminUser(String userName, String password) {
        Query q = Query.query(byUsername(userName));
        Update u = new Update()
                .setOnInsert(FIELD_ROLES, Collections.singleton("ADMIN"))
                .setOnInsert(FIELD_PASSWORD, password);
        WriteResult writeResult;
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            writeResult = mongoTemplate.upsert(q, u, MongoUser.class, securityConfig.getMongo().getCollection());
        } else {
            writeResult = mongoTemplate.upsert(q, u, MongoUser.class);
        }
        return writeResult.getN() > 0 && !writeResult.isUpdateOfExisting();

    }


    @Override
    public AttributedUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        username = username.toLowerCase(Locale.ROOT);

        final MongoUser mongoUser = getMongoUser(username);

        if (mongoUser == null) {
            log.debug("User {} not found", username, StringUtils.defaultString(securityConfig.getMongo().getCollection()));
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", username));
        }

        final MongoUserDetails userDetails = new MongoUserDetails(
                mongoUser.getUsername(),
                mongoUser.getPassword(),
                Collections2.transform(mongoUser.getRoles(),
                        role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(role, Locale.ROOT))
                )
        );
        userDetails.addAttributes(mongoUser.getAttributes());
        return userDetails;
    }



    private WriteResult updateMongoUser(String username, Update update) {
        return updateMongoUser(username, new Query(), update);
    }
    private WriteResult updateMongoUser(String username, Query query, Update update) {
        query.addCriteria(byUsername(username));

        WriteResult result;
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            result = mongoTemplate.updateFirst(query, update, securityConfig.getMongo().getCollection());
        } else {
            result = mongoTemplate.updateFirst(query, update, MongoUser.class);
        }
        return result;

    }

    private static Criteria byUsername(String username) {
        return Criteria.where("_id").is(username);
    }

    private MongoUser getMongoUser(String username) {
        MongoUser mongoUser;
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            mongoUser = mongoTemplate.findById(username, MongoUser.class, securityConfig.getMongo().getCollection());
        } else {
            mongoUser = mongoTemplate.findById(username, MongoUser.class);
        }
        return mongoUser;
    }

    public AttributedUserDetails createUserDetail(MongoUser user) {

        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            mongoTemplate.insert(user, securityConfig.getMongo().getCollection());
        } else {
            mongoTemplate.insert(user);
        }

        return loadUserByUsername(user.getUsername());
    }

    public MongoUser createPasswordRecoveryToken(String userName) {
        final MongoUser mongoUser = findMongoUser(userName);
        if (mongoUser == null) {
            return null;
        }

        final Date now = new Date(),
                expiry = DateUtils.addHours(now, 24);
        final String token = HashUtils.sha256(UUID.randomUUID() + mongoUser.getUsername());
        final MongoUser.PasswordRecovery recovery = new MongoUser.PasswordRecovery(token, now, expiry);

        final WriteResult result = updateMongoUser(mongoUser.getUsername(), Update.update(FIELD_RECOVERY, recovery));
        if (result.getN() == 1) {
            return getMongoUser(mongoUser.getUsername());
        } else {
            return null;
        }
    }

    public MongoUser findMongoUser(String userName) {
        MongoUser mongoUser = getMongoUser(userName);
        if (mongoUser != null) {
            return mongoUser;
        }

        final Query byMailQuery = new Query(Criteria.where("attributes.email").is(userName));
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            mongoUser = mongoTemplate.findOne(byMailQuery, MongoUser.class, securityConfig.getMongo().getCollection());
        } else {
            mongoUser = mongoTemplate.findOne(byMailQuery, MongoUser.class);
        }
        return mongoUser;
    }

    /**
     *
     * @param newPassword already hashed password
     */
    public boolean updatePassword(String userName, String newPassword, String recoveryToken) {
        return updateMongoUser(
                userName,
                Query.query(Criteria.where(FIELD_RECOVERY + "." + FIELD_TOKEN).is(recoveryToken)
                        .and(FIELD_RECOVERY + "." + FIELD_EXPIRES).gte(new Date())),
                Update.update(FIELD_PASSWORD, newPassword)
        ).getN() == 1;
    }

    /**
     * @param newPassword already hashed password
     */
    public boolean updatePassword(String userName, String newPassword) {
        return updateMongoUser(userName, Update.update(FIELD_PASSWORD, newPassword)).getN() == 1;
    }

    public boolean hasAccount(String userName) {
        return mongoTemplate.exists(new Query(byUsername(userName)), MongoUser.class);
    }

    public boolean updateRoles(String username, Set<String> roles) {
        return updateMongoUser(username, Update.update(FIELD_ROLES, roles)).getN() == 1;
    }

    public boolean deleteUser(String username) {
        return mongoTemplate.remove(new Query(byUsername(username)), MongoUser.class).getN() == 1;
    }

    public static class MongoUser extends io.redlink.smarti.model.SmartiUser {

        /**
         * SHA-2 hashed!
         */
        @Field(FIELD_PASSWORD)
        @JsonIgnore
        private String password;

        @Field(FIELD_ROLES)
        private Set<String> roles = new HashSet<>();

        @Field(FIELD_RECOVERY)
        @JsonIgnore
        private PasswordRecovery recovery = null;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        public PasswordRecovery getRecovery() {
            return recovery;
        }

        public void setRecovery(PasswordRecovery recovery) {
            this.recovery = recovery;
        }

        public static class PasswordRecovery {

            @Field(FIELD_TOKEN)
            private String token;
            private Date created;
            @Field(FIELD_EXPIRES)
            private Date expires;

            public PasswordRecovery() {
            }

            public PasswordRecovery(String token, Date created, Date expires) {
                this.token = token;
                this.created = created;
                this.expires = expires;
            }

            public String getToken() {
                return token;
            }

            public void setToken(String token) {
                this.token = token;
            }

            public Date getCreated() {
                return created;
            }

            public void setCreated(Date created) {
                this.created = created;
            }

            public Date getExpires() {
                return expires;
            }

            public void setExpires(Date expires) {
                this.expires = expires;
            }
        }
    }
}
