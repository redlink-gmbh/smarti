/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Intent;
import io.redlink.smarti.model.Token;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 */
@ApiModel
public class IntentResponse {

    @ApiModelProperty(value = "Tokens extracted")
    private List<Token> tokens = new ArrayList<>();

    @ApiModelProperty(value = "Templates for possible queries")
    private List<Intent> queryTemplates = new ArrayList<>();

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Intent> getQueryTemplates() {
        return queryTemplates;
    }

    public void setQueryTemplates(List<Intent> queryTemplates) {
        this.queryTemplates = queryTemplates;
    }

    public static IntentResponse from(Conversation conversation) {
        final IntentResponse response = new IntentResponse();

        response.tokens = conversation.getTokens();
        response.queryTemplates = conversation.getQueryTemplates();


        return response;
    }
}
