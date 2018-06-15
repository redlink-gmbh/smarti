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
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description="The analysis of a conversation based on the configuration of a client")
@Document
@CompoundIndexes(value={
        @CompoundIndex(def= "{'conversation': 1, 'date': 1}")
})
public class Analysis {

    /*
     * NOTE about JSON serialization: 
     * Used embedded into the ConversationData so we do not need id, conversation and date
     */
    @ApiModelProperty(hidden=true)
    @Id
    @JsonIgnore 
    private final ObjectId id;
    
    @ApiModelProperty(hidden=true)
    @Indexed
    @JsonIgnore
    private final ObjectId client;
    
    @ApiModelProperty(notes="The id of the conversation this analysis is about. Might be absent if serialized as part of a conversation")
    @Indexed
    private final ObjectId conversation;
    
    @ApiModelProperty(notes="Contextual information about the analysis (e.g. the analysed section of the channel)")
    @JsonInclude(Include.NON_NULL)
    private AnalysisContext context;
    
    @ApiModelProperty(notes="The last modification date of the analyzed conversation. Might be absent if serialized as part of a conversation")
    private final Date date;
    
    //TODO: maybe add additional data for user modified analysis
    
    @ApiModelProperty(value = "extracted tokens", required=true)
    private List<Token> tokens = new ArrayList<>();

    @ApiModelProperty(value = "Templates for possible queries", required=true)
    private List<Template> templates = new ArrayList<>();

    
    public Analysis(ObjectId client, ObjectId conversation, Date date) {
        this(null, client, conversation, date);
    }
    @JsonCreator
    public Analysis(@JsonProperty("conversation")ObjectId conversation, @JsonProperty("date")Date date){
        this(null,null,conversation,date);
    }
    @PersistenceConstructor
    public Analysis(ObjectId id, ObjectId client, ObjectId conversation, Date date) {
        this.id = id;
        this.client = client;
        assert conversation != null;
        this.conversation = conversation;
        assert date != null;
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

    public AnalysisContext getContext() {
        return context;
    }
    
    public void setContext(AnalysisContext context) {
        this.context = context;
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
    
    @ApiModel(description="The analysis context provides information about the section of a channel analysed.")
    public static class AnalysisContext {
        
        
        @ApiModelProperty(notes="The index of the message at the start of the analyzed section")
        final private int start;
        @ApiModelProperty(notes="The index of the message after the end of the analyzed section")
        final private int end;
        
        @ApiModelProperty(notes="The number of messages skipped for analysis within the analyzed section")
        @JsonInclude(Include.NON_NULL)
        private Integer skipped;
        
        @JsonCreator
        public AnalysisContext(@JsonProperty("start")int start, @JsonProperty("end")int end) {
            assert start >= 0;
            this.start = start;
            assert end >= start;
            this.end = end;
        }
        
        public int getStart() {
            return start;
        }
        
        public int getEnd() {
            return end;
        }

        public void setSkipped(Integer skipped) {
            this.skipped = skipped;
        }
        
        public Integer getSkipped() {
            return skipped;
        }
    }
}
