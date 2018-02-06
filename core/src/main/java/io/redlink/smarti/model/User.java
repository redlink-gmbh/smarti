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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.PersistenceConstructor;

/**
 * A User - a Customer - of Reisebuddy
 */
@ApiModel(description = "a user - taking part in a conversation")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User {

    @ApiModelProperty(value = "unique ID")
    private final String id;
    private String displayName;
    private String phoneNumber;
    private String email;
    @ApiModelProperty(notes = "the hometown of the user, used as fallback for travel-inquires")
    private String homeTown;

//    List<Recap> history;

    //List<Setting> temporalProfile;

    public User() {
        this(UUID.randomUUID().toString());
        setDisplayName("Anonymous User");
    }

    @JsonCreator
    @PersistenceConstructor
    public User(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHomeTown() {
        return homeTown;
    }

    public void setHomeTown(String homeTown) {
        this.homeTown = homeTown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", displayName=" + displayName + "]";
    }
    
}
