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

import com.google.common.collect.Collections2;
import com.mongodb.WriteResult;
import io.redlink.smarti.auth.AttributedUserDetails;
import io.redlink.smarti.auth.SecurityConfigurationProperties;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.utils.HashUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    private Logger log = LoggerFactory.getLogger(MongoUserDetailsService.class);

    private final MongoTemplate mongoTemplate;

    private final MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder;

    private final SecurityConfigurationProperties securityConfigurationProperties;

    public MongoUserDetailsService(MongoTemplate mongoTemplate, MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder, SecurityConfigurationProperties securityConfigurationProperties) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @PostConstruct
    protected void initialize() {
        // Check for at least one admin-user
        final long adminUsers = mongoTemplate.count(Query.query(Criteria.where(SmartiUser.FIELD_ROLES).is("ADMIN")), SmartiUser.class);
        if (adminUsers < 1) {
            log.debug("No admin-users found, creating");
            String adminUser = "admin";
            int i = 0;
            final String password = StringUtils.defaultString(securityConfigurationProperties.getMongo().getAdminPassword(), UUID.randomUUID().toString());
            final String encodedPassword = passwordEncoder.encodePassword(password);
            while (!createAdminUser(adminUser, encodedPassword)) {
                adminUser = String.format("admin%d", ++i);
            }
            if (StringUtils.isNotBlank(securityConfigurationProperties.getMongo().getAdminPassword())) {
                log.info("Created new Admin-User '{}' with password passed as 'security.config.mongo.admin-password'", adminUser);
            } else {
                log.error("Created new Admin-User '{}' with password '{}'", adminUser, password);
            }
        } else {
            log.debug("Found {} Admin-Users", adminUsers);
        }
    }

    private boolean createAdminUser(String login, String password) {
        Query q = Query.query(byLogin(login));
        Update u = new Update()
                .setOnInsert(SmartiUser.FIELD_ROLES, Collections.singleton("ADMIN"))
                .setOnInsert(SmartiUser.FIELD_PASSWORD, password);
        final WriteResult writeResult = mongoTemplate.upsert(q, u, SmartiUser.class);

        return writeResult.getN() > 0 && !writeResult.isUpdateOfExisting();
    }


    @Override
    public AttributedUserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
//        login = login.toLowerCase(Locale.ROOT);

        final SmartiUser smartiUser = getSmaritUser(login);

        if (smartiUser == null) {
            log.debug("User {} not found", login);
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", login));
        }

        final MongoUserDetails userDetails = new MongoUserDetails(
                smartiUser.getLogin(),
                smartiUser.getPassword(),
                Collections2.transform(smartiUser.getRoles(),
                        role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(role, Locale.ROOT))
                )
        );
        userDetails.addAttributes(smartiUser.getProfile());
        return userDetails;
    }



    private WriteResult updateMongoUser(String login, Update update) {
        return updateMongoUser(login, new Query(), update);
    }
    private WriteResult updateMongoUser(String login, Query query, Update update) {
        query.addCriteria(byLogin(login));

        return mongoTemplate.updateFirst(query, update, SmartiUser.class);
    }

    private static Criteria byLogin(String login) {
        return Criteria.where("_id").is(login);
    }

    private SmartiUser getSmaritUser(String login) {
        return mongoTemplate.findById(login, SmartiUser.class);
    }

    public AttributedUserDetails createUserDetail(SmartiUser user) {
        mongoTemplate.insert(user);

        return loadUserByUsername(user.getLogin());
    }

    public SmartiUser createPasswordRecoveryToken(String login) {
        final SmartiUser mongoUser = findUser(login);
        if (mongoUser == null) {
            return null;
        }

        final Date now = new Date(),
                expiry = DateUtils.addHours(now, 24);
        final String token = HashUtils.sha256(UUID.randomUUID() + mongoUser.getLogin());
        final SmartiUser.PasswordRecovery recovery = new SmartiUser.PasswordRecovery(token, now, expiry);

        final WriteResult result = updateMongoUser(mongoUser.getLogin(), Update.update(SmartiUser.FIELD_RECOVERY, recovery));
        if (result.getN() == 1) {
            return getSmaritUser(mongoUser.getLogin());
        } else {
            return null;
        }
    }

    /**
     * Find a user. If a user with the provided parameter is not found, this method looks for
     * a user with a matching email-address.
     * @param login the login (username) or email-address
     */
    public SmartiUser findUser(String login) {
        SmartiUser mongoUser = getSmaritUser(login);
        if (mongoUser != null) {
            return mongoUser;
        }

        final Query byMailQuery = new Query(Criteria.where(SmartiUser.PROFILE_FIELD(SmartiUser.ATTR_EMAIL)).is(login));
        mongoUser = mongoTemplate.findOne(byMailQuery, SmartiUser.class);

        return mongoUser;
    }

    /**
     *
     * @param newPassword already hashed password
     */
    public boolean updatePassword(String login, String newPassword, String recoveryToken) {
        return updateMongoUser(
                login,
                Query.query(Criteria.where(SmartiUser.FIELD_RECOVERY + "." + SmartiUser.FIELD_TOKEN).is(recoveryToken)
                        .and(SmartiUser.FIELD_RECOVERY + "." + SmartiUser.FIELD_EXPIRES).gte(new Date())),
                Update.update(SmartiUser.FIELD_PASSWORD, newPassword)
        ).getN() == 1;
    }

    /**
     * @param newPassword already hashed password
     */
    public boolean updatePassword(String login, String newPassword) {
        return updateMongoUser(login, Update.update(SmartiUser.FIELD_PASSWORD, newPassword)).getN() == 1;
    }

    public boolean hasAccount(String login) {
        return mongoTemplate.exists(new Query(byLogin(login)), SmartiUser.class);
    }

    public boolean updateRoles(String login, Set<String> roles) {
        return updateMongoUser(login, Update.update(SmartiUser.FIELD_ROLES, roles)).getN() == 1;
    }

    public boolean deleteUser(String login) {
        return mongoTemplate.remove(new Query(byLogin(login)), SmartiUser.class).getN() == 1;
    }

}
