/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Token;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 */
@ApiModel
public class TemplateResponse {

    @ApiModelProperty(value = "Tokens extracted")
    private List<Token> tokens = new ArrayList<>();

    @ApiModelProperty(value = "Templates for possible queries")
    private List<Template> templates = new ArrayList<>();

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public static TemplateResponse from(Conversation conversation) {
        final TemplateResponse response = new TemplateResponse();

        response.tokens = conversation.getTokens();
        response.templates = conversation.getQueryTemplates();


        return response;
    }
}
