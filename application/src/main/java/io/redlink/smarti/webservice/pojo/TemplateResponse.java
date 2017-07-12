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
        response.templates = conversation.getTemplates();


        return response;
    }
}
