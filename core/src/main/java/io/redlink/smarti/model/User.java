/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.redlink.smarti.model.profile.Recap;
import io.redlink.smarti.model.profile.Setting;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A User - a Customer - of Reisebuddy
 */
@ApiModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User {

    @ApiModelProperty(value = "unique ID", required = true)
    String id;
    String displayName;
    String phoneNumber;
    String email;
    @ApiModelProperty(notes = "the hometown of the user, used as fallback for travel-inquires")
    String homeTown;

    List<Recap> history;

    List<Setting> temporalProfile;

    public User() {
        this(UUID.randomUUID().toString());
        setDisplayName("Anonymous User");
    }

    @JsonCreator
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
}
