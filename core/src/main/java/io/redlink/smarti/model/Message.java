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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A message - part of the communication between Customer and Agent
 */
@ApiModel
public class Message implements Comparable<Message> {

    public enum Origin {
        User,
        Agent
    }

    @ApiModelProperty("the id of the message")
    private String id;
    @ApiModelProperty
    private Date time = new Date();
    @ApiModelProperty("origin of the message")
    private Origin origin = Origin.User;
    @ApiModelProperty(value = "message content", required = true)
    private String content;
    @ApiModelProperty("the user who sent the message")
    private User user = null;
    @ApiModelProperty(name = "private", value = "marks a private message (not searchable)")
    @JsonProperty("private") @Field("private")
    private boolean _private = false;
    @ApiModelProperty(value = "votes for this message - how often this message was considered helpful")
    private int votes = 0;
    @ApiModelProperty(value = "message metadata")
    @JsonInclude(content=Include.NON_EMPTY) //exclude if empty
    private final Map<String, String> metadata = new HashMap<>();

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isPrivate() {
        return _private;
    }

    public void setPrivate(boolean _private) {
        this._private = _private;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public int compareTo(Message other) {
        return time.compareTo(other.time);
    }

    @Override
    public String toString() {
        return String.format("%.1s: \"%s\" (%tc)", origin, StringUtils.abbreviate(content, 20), time);
    }
}
