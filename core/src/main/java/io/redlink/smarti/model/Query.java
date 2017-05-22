/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A Query to one of the external services
 */
@ApiModel
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

    @ApiModelProperty(notes = "state of this query")
    private State state = State.Suggested;

    public Query() {
        this(null);
    }

    public Query(String creator) {
        this.creator = creator;
    }

    public float getConfidence() {
        return confidence;
    }

    public Query setConfidence(float confidence) {
        this.confidence = confidence;
        return this;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public Query setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Query setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public Query setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public boolean isInlineResultSupport() {
        return inlineResultSupport;
    }

    public Query setInlineResultSupport(boolean inlineResultSupport) {
        this.inlineResultSupport = inlineResultSupport;
        return this;
    }

    public State getState() {
        return state;
    }

    public Query setState(State state) {
        this.state = state;
        return this;
    }
}
