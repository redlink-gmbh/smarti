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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.redlink.utils.HashUtils;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
public class AuthToken {

    public static final String TOKEN_SEPARATOR = "/";

    @Id
    private final String id;

    @JsonIgnore
    @Indexed
    private final ObjectId clientId;

    @JsonIgnore
    private String secret;

    @Indexed(unique = true, sparse = true)
    private String token;

    private String label;

    private Date created = new Date();

    public AuthToken(ObjectId clientId) {
        this(null, clientId);
    }

    @JsonCreator
    private AuthToken(@JsonProperty("id") String id) {
        this(id, null);
    }

    @PersistenceConstructor
    private AuthToken(String id, ObjectId clientId) {
        this.id = id;
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public ObjectId getClientId() {
        return clientId;
    }

    public String getLabel() {
        return label;
    }

    public AuthToken setLabel(String label) {
        this.label = label;
        return this;
    }

    public Date getCreated() {
        return created;
    }

    public AuthToken setCreated(Date created) {
        this.created = created;
        return this;
    }

    public AuthToken setSecret(String secret) {
        this.secret = secret;
        this.token = HashUtils.sha1(getId()) + TOKEN_SEPARATOR + secret;
        return this;
    }
}
