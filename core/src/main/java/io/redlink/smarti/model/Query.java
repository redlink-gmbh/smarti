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


import java.util.Date;

import org.springframework.data.annotation.PersistenceConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.redlink.smarti.model.config.ComponentConfiguration;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A Query to one of the external services
 */
@ApiModel
@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="_class", defaultImpl=Query.class)
public class Query {

    @ApiModelProperty(value = "name of the service", notes = "human readable name of the query", required = true)
    private String displayTitle;
    @ApiModelProperty(notes = "confidence that this query is useful")
    private float confidence;
    @ApiModelProperty(value = "query url", notes = "the URL to run the query (incl. all parameters)", required = true)
    private String url;
    @ApiModelProperty(value = "queryCreator", notes = "the module that created the query", required = true)
    private String creator;
    @ApiModelProperty(value = "supports inline results", notes = "if the query/creator supports inline results")
    private boolean inlineResultSupport = false;

    @ApiModelProperty(value = "created", notes = "The creation date of this query")
    private Date created;
    
    @ApiModelProperty(notes = "state of this query")
    private State state = State.Suggested;

    public Query() {
        this(null);
    }

    public Query(String creator) {
        this(creator, new Date());
    }
    
    @PersistenceConstructor
    @JsonCreator
    protected Query(@JsonProperty("creator")String creator, @JsonProperty("created")Date created) {
        this.creator = creator;
        this.created = created;
    }

    @JsonGetter("confidence")
    public float getConfidence() {
        return confidence;
    }
    /**
     * The creation date
     * @return the date or <code>null</code> if unknown (for old data)
     */
    @JsonGetter("created")
    public Date getCreated() {
        return created;
    }

    @JsonSetter("confidence")
    public Query setConfidence(float confidence) {
        this.confidence = confidence;
        return this;
    }

    @JsonGetter("displayTitle")
    public String getDisplayTitle() {
        return displayTitle;
    }

    @JsonSetter("displayTitle")
    public Query setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
        return this;
    }

    @JsonGetter("url")
    public String getUrl() {
        return url;
    }

    @JsonSetter("url")
    public Query setUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonGetter("creator")
    public String getCreator() {
        return creator;
    }

    @JsonSetter("creator")
    public Query setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    @JsonGetter("inlineResultSupport")
    public boolean isInlineResultSupport() {
        return inlineResultSupport;
    }

    @JsonSetter("inlineResultSupport")
    public Query setInlineResultSupport(boolean inlineResultSupport) {
        this.inlineResultSupport = inlineResultSupport;
        return this;
    }

    @JsonGetter("state")
    public State getState() {
        return state;
    }

    @JsonSetter("state")
    public Query setState(State state) {
        this.state = state;
        return this;
    }
}
