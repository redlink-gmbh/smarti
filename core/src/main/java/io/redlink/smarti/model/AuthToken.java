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

import java.util.Date;

@Document
public class AuthToken {

    @Id
    private String token;

    @JsonIgnore
    @Indexed
    private ObjectId clientId;

    private String label;

    private Date created = new Date();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ObjectId getClientId() {
        return clientId;
    }

    public AuthToken setClientId(ObjectId clientId) {
        this.clientId = clientId;
        return this;
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
}
