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

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "users")
public class SmartiUser {

    public static final String ATTR_DISPLAY_NAME = "displayName";
    public static final String ATTR_EMAIL = "email";
    public static final String FIELD_CLIENTS = "clients";
    public static final String FIELD_ATTRIBUTES = "attributes";

    public static String ATTR_FIELD(String ATTR) {
        return FIELD_ATTRIBUTES + "." + ATTR;
    }

    @Id
    private String username;

    @Indexed(sparse = true)
    @Field(FIELD_CLIENTS)
    private Set<ObjectId> clients = new HashSet<>();

    @Field(FIELD_ATTRIBUTES)
    private Map<String, String> attributes = new HashMap<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<ObjectId> getClients() {
        return clients;
    }

    public void setClients(Set<ObjectId> clients) {
        this.clients = clients;
    }

    public String getDisplayName() {
        return attributes.get(ATTR_DISPLAY_NAME);
    }

    public void setDisplayName(String displayName) {
        attributes.put(ATTR_DISPLAY_NAME, displayName);
    }

    public String getEMailAddress() {
        return attributes.get(ATTR_EMAIL);
    }

    public void setEMailAddress(String eMailAddress) {
        attributes.put(ATTR_EMAIL, eMailAddress);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
