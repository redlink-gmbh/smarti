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
package io.redlink.smarti.services;

import io.redlink.smarti.auth.AttributedUserDetails;
import io.redlink.smarti.auth.mongo.MongoPasswordHasherConfiguration;
import io.redlink.smarti.auth.mongo.MongoUserDetailsService;
import io.redlink.smarti.model.SmartiUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@ConditionalOnBean(MongoUserDetailsService.class)
public class AccountService {

    private Logger log = LoggerFactory.getLogger(AccountService.class);

    private final MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder;
    private final MongoUserDetailsService userDetailsService;

    public AccountService(MongoUserDetailsService userDetailsService, MongoPasswordHasherConfiguration.PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    public AttributedUserDetails createAccount(String userName, String mail) {
        return createAccount(userName, mail, null);
    }

    public AttributedUserDetails createAccount(String userName, String mail, String password) {
        SmartiUser user = new SmartiUser();
        user.setLogin(userName);
        user.setPassword(passwordEncoder.encodePassword(password));
        user.setRoles(Collections.emptySet());
        user.setProfile(Collections.singletonMap(AttributedUserDetails.ATTR_EMAIL, mail));

        return userDetailsService.createUserDetail(user);
    }

    public boolean startPasswordRecovery(String login) {
        final SmartiUser mongoUser = userDetailsService.createPasswordRecoveryToken(login);
        if (mongoUser != null) {
            final SmartiUser.PasswordRecovery passwordRecovery = mongoUser.getRecovery();
            // TODO: Send a mail
            log.info("Created password recovery link for user '{}': <http://localhost:8080/#!/login?user={}&token={}>, valid until {}",
                    mongoUser.getLogin(), mongoUser.getLogin(), passwordRecovery.getToken(), passwordRecovery.getExpires());
        }
        return mongoUser != null;
    }

    public boolean completePasswordRecovery(String userName, String newPassword, String recoveryToken) {
        return userDetailsService.updatePassword(userName, passwordEncoder.encodePassword(newPassword), recoveryToken);
    }

    public boolean hasAccount(String userName) {
        return userDetailsService.hasAccount(userName);
    }

    public void setPassword(String username, String newPassword) {
        userDetailsService.updatePassword(username, passwordEncoder.encodePassword(newPassword));
    }

    public boolean setRoles(String username, Set<String> roles) {
        return userDetailsService.updateRoles(username, roles);
    }

    public boolean deleteUser(String username) {
        return userDetailsService.deleteUser(username);
    }
}
