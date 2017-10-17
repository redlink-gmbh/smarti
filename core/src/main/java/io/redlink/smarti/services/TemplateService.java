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
package io.redlink.smarti.services;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 */
@Component
public class TemplateService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final List<TemplateBuilder> templateBuilders;

    public TemplateService() {
        this.templateBuilders = Collections.emptyList();
    }

    @Autowired(required = false)
    public TemplateService(Optional<List<TemplateBuilder>> templateBuilders) {
        log.debug("Start TempalteService (TemplateBuilders: {})", templateBuilders);
        this.templateBuilders = templateBuilders.orElse(Collections.emptyList());
    }

    /**
     * Builds and updates {@link Template}s for the parsed conversation by
     * considering {@link Token}s from {@link Message}s with in index starting
     * of the parsed <code>startMsgIdx</code>. {@link Token}s with an 
     * {@link Token.Origin#Agent} MUST BE considered independent from the {@link Token#getMessageIdx()}
     * @param client the client to build the templates for 
     * @param con the conversation
     * @param analysis the analysis results for the conversation
     */
    public void updateTemplates(Client client, Conversation con, Analysis analysis) {
        //TODO: get template configuration for parsed client
        log.debug("Update QueryTemplates for {}", con);
        final long templStart = System.currentTimeMillis();
        templateBuilders.forEach(builder -> builder.updateTemplate(con, analysis));
        log.debug("Created QueryTemplates for {} in {}ms", con, System.currentTimeMillis() - templStart);
    }

}
