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

package io.redlink.smarti.query.conversation;


import io.redlink.smarti.model.Query;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
public class RocketChatSearchQuery extends Query {

    @JsonProperty("payload")
    private final Map<String,Object> payload = new HashMap<>();
    @JsonProperty("params")
    private final Map<String,Object> params = new HashMap<>();
    @JsonProperty("keywords")
    private final Collection<String> keywords = new LinkedHashSet<>();
    @JsonProperty("terms")
    private final Collection<String> terms = new LinkedHashSet<>();
    @ApiModelProperty(notes="The IDs of users that participate in the current context of the conversation")
    @JsonProperty("filterQueries")
    private final Set<String> users = new HashSet<>();
    
    private final Collection<String> contextMsgs = new LinkedHashSet<>();
    @JsonProperty("contextQuery")
    private final List<ContextTerm> contextQuery = new LinkedList<>();
    
    public RocketChatSearchQuery(@JsonProperty("creator") String creator) {
        super(creator);
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> defaults) {
        this.payload.clear();
        if(defaults != null){
            this.payload.putAll(defaults);
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params.clear();
        if(params != null){
            this.params.putAll(params);
        }
    }
    
    @JsonIgnore
    public void setParam(String param, Object value){
        if(StringUtils.isEmpty(param)){
            return;
        }
        if(value == null){
            this.params.remove(param);
        } else {
            this.params.put(param, value);
        }
    }
    
    public void setKeywords(Collection<String> strs) {
        keywords.clear();
        if(strs != null){
            keywords.addAll(strs);
        }
    }
    
    public Collection<String> getKeywords() {
        return keywords;
    }

    public void setTerms(Collection<String> strs) {
        terms.clear();
        if(strs != null){
            terms.addAll(strs);
        }
    }
    
    public Collection<String> getTerms() {
        return terms;
    }

    public Set<String> getUsers() {
        return users;
    }
    
    public void setUsers(Collection<String> users) {
        this.users.clear();
        if(users != null){
            this.users.addAll(users);
        }
    }
    
    @JsonIgnore
    public void addUser(String user){
        this.users.add(user);
    }

    public void setContextMsgs(Collection<String> msgIds){
        this.contextMsgs.clear();
        if(msgIds != null){
            this.contextMsgs.addAll(msgIds);
        }
    }
    
    public Collection<String> getContextMsgs() {
        return contextMsgs;
    }

    @JsonIgnore
    public void addContextMsg(String id){
        if(id != null){
            this.contextMsgs.add(id);
        }
    }
    
    public List<ContextTerm> getContextQuery() {
        return contextQuery;
    }
    
    public void setContextQuery(List<ContextTerm> terms){
        contextQuery.clear();
        if(terms != null){
            contextQuery.addAll(terms);
        }
    }
    
    @JsonIgnore
    public void addContextQueryTerm(String term, float relevance){
        if(StringUtils.isNotEmpty(term) && relevance > 0f){
            contextQuery.add(new ContextTerm(term, relevance));
        }
    }
    
    public static class ContextTerm {
        
        @JsonProperty
        final String term;
        @JsonProperty
        final float relevance;
        
        @JsonCreator
        public ContextTerm(@JsonProperty("term")String term, @JsonProperty("relevance")float relevance){
            this.term = term;
            this.relevance = relevance;
        }
        
        public String getTerm() {
            return term;
        }
        
        public float getRelevance() {
            return relevance;
        }
        
    }
}
