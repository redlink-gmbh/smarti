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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@ApiModel
@Document
@CompoundIndexes(value={
        @CompoundIndex(def= "{'conversation': 1, 'date': 1")
})
public class Analysis {

    /*
     * NOTE about JSON serialization: 
     * Used embedded into the ConversationData so we do not need id, conversation and date
     */
    @Id
    @JsonIgnore 
    private final ObjectId id;
    
    @Indexed
    @JsonIgnore
    private final ObjectId client;
    
    @Indexed
    @JsonIgnore
    private final ObjectId conversation;
    
    @JsonIgnore
    private final Date date;
    
    //TODO: maybe add additional data for user modified analysis
    
    @ApiModelProperty(value = "extracted tokens")
    private List<Token> tokens = new ArrayList<>();

    @ApiModelProperty(value = "Templates for possible queries")
    private List<Template> templates = new ArrayList<>();

    
    @PersistenceConstructor
    public Analysis(ObjectId clientId, ObjectId conversationId, Date date) {
        this(null, clientId, conversationId, date);
    }
    public Analysis(ObjectId id, ObjectId clientId, ObjectId conversationId, Date date) {
        this.id = id;
        this.conversation = conversationId;
        this.client = clientId;
        this.date = date;
    }

    public ObjectId getId() {
        return id;
    }
    
    public ObjectId getClient() {
        return client;
    }
    
    public ObjectId getConversation() {
        return conversation;
    }
    
    public Date getDate() {
        return date;
    }
    
    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    @Override
    public String toString() {
        return "Analysis [" + tokens.size() + "tokens, " + templates.size() + " templates]";
    }
}
