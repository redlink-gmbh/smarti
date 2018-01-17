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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Conversation object.
 *
 * <strong>ATTENTION</strong> If you change something here, you need to update {@link io.redlink.smarti.repositories.ConversationRepositoryImpl#saveIfNotLastModifiedAfter(io.redlink.smarti.model.Conversation, java.util.Date)}!
 */
@ApiModel(description = "a conversation, the central entitiy in smarti")
@Document(collection = "conversations")
public class Conversation {

    @Id
    @ApiModelProperty
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Indexed
    @JsonIgnore
    private String channelId;

    @Indexed
    @JsonIgnore
    private ObjectId owner;
    
    @ApiModelProperty
    private ConversationMeta meta = new ConversationMeta();

    @JsonProperty(required = true)
    @ApiModelProperty(required = true)
    private User user = new User();

    @ApiModelProperty(required = true, value = "List of Messages")
    private final List<Message> messages = new LinkedList<>();

//    @ApiModelProperty(required = true, value = "the analysis results")
//    private Analysis analysis = new Analysis();

    @ApiModelProperty
    private Context context = new Context();

    private Date lastModified = null;

    public Conversation(){
        this(null, null);
    }
    
    @PersistenceConstructor
    public Conversation(ObjectId id, ObjectId owner){
        this.id = id;
        this.owner = owner;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    /**
     * @return
     * @deprecated use #getOwner() instead
     */
    @Deprecated
    @JsonIgnore
    public ObjectId getClientId() {
        return getOwner();
    }

    /**
     * @deprecated use {@link #setOwner(ObjectId)} instead
     */
    @Deprecated
    public void setClientId(ObjectId clientId) {
        setOwner(clientId);;
    }

    public ObjectId getOwner() {
        return owner;
    }
    
    public void setOwner(ObjectId owner) {
        this.owner = owner;
    }
    
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public ConversationMeta getMeta() {
        return meta;
    }

    public void setMeta(ConversationMeta meta) {
        this.meta = meta;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Read-/writeable list of {@link Message}s
     * @return the messages of this conversation
     */
    public List<Message> getMessages() {
        return messages;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "Conversation [id=" + id + ", channelId=" + channelId + ", user=" + user + ", lastModified="
                + lastModified + ", " + messages.size() + " messages]";
    }
    
}
