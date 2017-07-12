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
