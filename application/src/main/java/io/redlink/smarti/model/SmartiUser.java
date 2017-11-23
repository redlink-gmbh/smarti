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
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

@Document(collection = "users")
public class SmartiUser {

    public static final String ATTR_DISPLAY_NAME = "name";
    public static final String ATTR_EMAIL = "email";
    public static final String FIELD_CLIENTS = "clients";
    public static final String FIELD_PROFILE = "profile";
    public static final String FIELD_RECOVERY = "recovery";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_ROLES = "roles";
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_EXPIRES = "expires";

    public static String PROFILE_FIELD(String ATTR) {
        return FIELD_PROFILE + "." + ATTR;
    }

    @Id
    private String login;

    @Field(FIELD_PASSWORD)
    @JsonIgnore
    private String password;

    @Field(FIELD_ROLES)
    private Set<String> roles = new HashSet<>();

    @Field(FIELD_RECOVERY)
    @JsonIgnore
    private SmartiUser.PasswordRecovery recovery = null;

    @Indexed(sparse = true)
    @Field(FIELD_CLIENTS)
    private Set<ObjectId> clients = new HashSet<>();

    @Field(FIELD_PROFILE)
    private Map<String, String> profile = new HashMap<>();


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Set<ObjectId> getClients() {
        return clients;
    }

    public void setClients(Set<ObjectId> clients) {
        this.clients = clients;
    }

    public Map<String, String> getProfile() {
        return profile;
    }

    public void setProfile(Map<String, String> profile) {
        this.profile = profile;
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
