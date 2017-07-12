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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * A slot represents an {@link Token} with a Role in the context of an {@link Template}.
 */
@ApiModel
public class Slot {

    @JsonProperty("role")
    private String role;
    @JsonProperty("tokenType")
    private Token.Type tokenType;
    private boolean required = false;
    private int tokenIndex = -1;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String inquiryMessage;

    /**
     * Creates an optional QuerySlot
     */
    @JsonCreator
    @PersistenceConstructor
    public Slot(@JsonProperty("role") String role, @JsonProperty("tokenType") Token.Type tokenType) {
        this(role, tokenType, null, false);
    }
    
    /**
     * Creates an QuerySlot
     */
    public Slot(String role, Token.Type tokenType, String inquiryMessage, boolean required) {
        this.role = role;
        this.tokenType = tokenType;
        this.required = required;
        this.inquiryMessage = inquiryMessage;
    }

    public String getRole() {
        return role;
    }

    public Token.Type getTokenType() {
        return tokenType;
    }

    public int getTokenIndex() {
        return tokenIndex;
    }

    public void setTokenIndex(int tokenIndex) {
        this.tokenIndex = tokenIndex;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getInquiryMessage() {
        return inquiryMessage;
    }

    public void setInquiryMessage(String inquiryMessage) {
        this.inquiryMessage = inquiryMessage;
    }

    @Override
    public String toString() {
        return "Slot [role=" + role + ", type=" + tokenType + ", req=" + required + ", idx=" + tokenIndex + "]";
    }
    
    
}
