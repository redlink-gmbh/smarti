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
    @ApiModelProperty(readOnly=true,notes="Server assigned ID of the conversation")
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

//    NOTE: removed with 0.7.0 as Smarti does no longer manage mappings of Conversations to Channels.
//    This is now the responsibility of the client (e.g. Rocket.Chat widget)
//    @Indexed
//    @JsonIgnore
//    private String channelId;

    @ApiModelProperty(notes="The Smarti client owning this conversation. Set during creation. MUST NOT be changed "
            + "afterwadrs. If the authenticated user is assigned to a single client (always the case for tokens) the "
            + "owner is set by the server. If a owner is parsed it MUST correspond to one of the clients the "
            + "authenticated user is assigned to.")
    @JsonIgnore
    @Indexed
    private ObjectId owner;
    
    @ApiModelProperty
    private ConversationMeta meta = new ConversationMeta();

    @JsonProperty(required = true)
    @ApiModelProperty(required = true, notes="Information about the user that created this conversation. "
            + "Represents the user of the chat system and NOT the Smarti user")
    private User user = new User();

    @ApiModelProperty(required = true, value = "List of Messages")
    private final List<Message> messages = new LinkedList<>();

//    NOTE: removed with 0.7.0: Analysis is now stored in an own collection. Mainly because one
//    conversation might have different analysis for clients with different configurations.
//    ConversationData still allows for sending conversation data with analysis to clients.
//    @ApiModelProperty(required = true, value = "the analysis results")
//    private Analysis analysis = new Analysis();

    @ApiModelProperty(notes="Contextual information aboout the conversation")
    private Context context = new Context();

    @ApiModelProperty(readOnly=true,notes="Server assigned modification date")
    @Indexed
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
    
//    public String getChannelId() {
//        return channelId;
//    }
//
//    public void setChannelId(String channelId) {
//        this.channelId = channelId;
//    }
//
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
        return "Conversation [id=" + id + ", owner=" + owner + ", user=" + user + ", lastModified="
                + lastModified + ", " + messages.size() + " messages]";
    }
    
}
