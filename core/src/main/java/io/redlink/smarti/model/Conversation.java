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
import java.util.List;

/**
 * Conversation object.
 *
 * <strong>ATTENTION</strong> If you change something here, you need to update {@link io.redlink.smarti.repositories.ConversationRepositoryImpl#saveIfNotLastModifiedAfter(io.redlink.smarti.model.Conversation, java.util.Date)}!
 */
@ApiModel
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
    
    @ApiModelProperty(value = "metadata")
    private ConversationMeta meta = new ConversationMeta();

    @JsonProperty(required = true)
    @ApiModelProperty(required = true)
    private User user = new User(); // TODO: needs discussion for REISEBUDDY-28

    @ApiModelProperty(required = true, value = "List of Messages")
    private List<Message> messages = new ArrayList<>();

    @ApiModelProperty(required = true, value = "the analysis results")
    private Analysis analysis = new Analysis();

    @ApiModelProperty(value = "conversation context")
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

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    /**
     * @deprecated use {@link Analysis#getTokens()}
     */
    @Deprecated
    @JsonIgnore
    public List<Token> getTokens() {
        return analysis.getTokens();
    }

    @Deprecated
    public void setTokens(List<Token> tokens) {
        this.analysis.setTokens(tokens);
    }

    /**
     * @deprecated use {@link Analysis#getTemplates()}
     */
    @Deprecated
    @JsonIgnore
    public List<Template> getTemplates() {
        return analysis.getTemplates();
    }

    @Deprecated
    public void setQueryTemplates(List<Template> queryTemplates) {
        this.analysis.setTemplates(queryTemplates);
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
