/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A result/preview of a Query
 */
@ApiModel
public abstract class Result {

    @ApiModelProperty(required = true)
    @JsonProperty("creator")
    private String creator;

    @ApiModelProperty(value = "reply", notes = "a suggested answer, ready to be sent to the customer")
    private String replySuggestion;

    protected Result(String creator) {
        this.creator = creator;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getReplySuggestion() {
        return replySuggestion;
    }

    public void setReplySuggestion(String replySuggestion) {
        this.replySuggestion = replySuggestion;
    }

}
