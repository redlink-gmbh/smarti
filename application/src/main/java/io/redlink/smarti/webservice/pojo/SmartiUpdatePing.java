/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;

/**
 */
@ApiModel
public class SmartiUpdatePing {

    @ApiModelProperty
    @JsonSerialize(using = ToStringSerializer.class)
    private final ObjectId conversationId;

    @ApiModelProperty
    private final String token;

    @JsonCreator
    public SmartiUpdatePing(@JsonProperty ObjectId conversationId, @JsonProperty String token) {
        this.conversationId = conversationId;
        this.token = token;
    }

    public ObjectId getConversationId() {
        return conversationId;
    }

    public String getToken() {
        return token;
    }
}
