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
public class Message {
    
    public static interface Metadata {
        
        /**
         * Boolean switch that allows to deactivate processing of the message content
         */
        String SKIP_ANALYSIS = "skipAnalysis";
    }

    public enum Origin {
        User,
        Agent,
        Bot
    }

    @ApiModelProperty(notes="the id of the message. If not set by the client this will be assigned by the server on creation", required=true)
    private String id;
    @ApiModelProperty(notes="time when this message was sent")
    private Date time = new Date();
    @ApiModelProperty("origin of the message")
    private Origin origin = Origin.User;
    @ApiModelProperty(value = "message content", required = true)
    private String content;
    @ApiModelProperty(notes="The user sending this message")
    private User user = null;
    @ApiModelProperty(name = "private", value = "marks a private message (not searchable)", required=false)
    @JsonProperty("private") @Field("private")
    private boolean _private = false;
    @ApiModelProperty(value = "votes for this message - how often this message was considered helpful", required=false)
    private int votes = 0;
    @ApiModelProperty(value = "message metadata", notes="Allows for storing additional information for this message", required=false)
    @JsonInclude(content=Include.NON_EMPTY) //exclude if empty
    private final Map<String, Object> metadata = new HashMap<>();

    public Message() {
        this(null);
    }
    
    public Message(String id) {
        this.id = id;
    }
    
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("%.1s: \"%s\" (%tc)", origin, StringUtils.abbreviate(content, 20), time);
    }
}
