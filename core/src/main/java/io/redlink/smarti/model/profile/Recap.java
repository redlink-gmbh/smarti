/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model.profile;

import io.redlink.smarti.model.MessageTopic;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 */
@ApiModel
public abstract class Recap {

    @ApiModelProperty("the source of the recap")
    private String conversationId;
    @ApiModelProperty
    private Date date;
    @ApiModelProperty("topic of the conversation")
    private MessageTopic topic;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MessageTopic getTopic() {
        return topic;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }
}
