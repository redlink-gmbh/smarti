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
import io.redlink.smarti.auth.SecurityConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 */
@Service
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
public class MongoUserDetailsService implements UserDetailsService {
    private static final String USER_COLLECTION = "users";

    private Logger log = LoggerFactory.getLogger(MongoUserDetailsService.class);

    private final MongoTemplate mongoTemplate;

    private final SecurityConfigurationProperties securityConfig;

    public MongoUserDetailsService(MongoTemplate mongoTemplate, SecurityConfigurationProperties securityConfig) {
        this.mongoTemplate = mongoTemplate;
        this.securityConfig = securityConfig;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        username = username.toLowerCase(Locale.ROOT);

        final MongoUser mongoUser;
        if (StringUtils.isNotBlank(securityConfig.getMongo().getCollection())) {
            mongoUser = mongoTemplate.findById(username, MongoUser.class, securityConfig.getMongo().getCollection());
        } else {
            mongoUser = mongoTemplate.findById(username, MongoUser.class);
        }

        if (mongoUser == null) {
            log.debug("User {} not found in {}", username, StringUtils.defaultString(securityConfig.getMongo().getCollection(), USER_COLLECTION));
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", username));
        }

        final MongoUserDetails userDetails = new MongoUserDetails(username, mongoUser.getPassword(), Collections2.transform(mongoUser.getRoles(),
                role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
        );
        userDetails.addAttributes(mongoUser.getAttributes());
        return userDetails;
    }

    @Document(collection = USER_COLLECTION)
    private static class MongoUser {

        @Id
        private String username;

        /**
         * SHA-2 hashed!
         */
        private String password;

        private Set<String> roles = new HashSet<>();

        private Map<String, String> attributes = new HashMap<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

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

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }
}
