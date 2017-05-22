/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.redlink.smarti.model.MessageTopic;
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
    @ApiModelProperty(required = true)
    @JsonProperty("topic")
    private MessageTopic topic;

    @ApiModelProperty(value = "reply", notes = "a suggested answer, ready to be sent to the customer")
    private String replySuggestion;

    protected Result(String creator, MessageTopic topic) {
        this.creator = creator;
        this.topic = topic;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public MessageTopic getTopic() {
        return topic;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }

    public String getReplySuggestion() {
        return replySuggestion;
    }

    public void setReplySuggestion(String replySuggestion) {
        this.replySuggestion = replySuggestion;
    }

}
