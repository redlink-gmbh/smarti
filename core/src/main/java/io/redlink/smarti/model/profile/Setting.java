/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model.profile;

import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.values.DateValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 */
@ApiModel
public class Setting {

    @ApiModelProperty("time-boundary for the setting")
    private DateValue start;
    @ApiModelProperty("time-boundary for the setting")
    private DateValue end;

    @ApiModelProperty("source of the setting")
    private String conversationId;
    @ApiModelProperty("token")
    private Token token;

    public DateValue getStart() {
        return start;
    }

    public void setStart(DateValue start) {
        this.start = start;
    }

    public DateValue getEnd() {
        return end;
    }

    public void setEnd(DateValue end) {
        this.end = end;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }
}
