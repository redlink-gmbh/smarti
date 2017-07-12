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
    private final String channelId;

    @ApiModelProperty
    private final String token;

    @JsonCreator
    public SmartiUpdatePing(@JsonProperty ObjectId conversationId,
                            @JsonProperty String channelId,
                            @JsonProperty String token) {
        this.conversationId = conversationId;
        this.channelId = channelId;
        this.token = token;
    }

    public ObjectId getConversationId() {
        return conversationId;
    }

    public String getToken() {
        return token;
    }

    public String getChannelId() {
        return channelId;
    }
}
