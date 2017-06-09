/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Template;

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
     * considering {@link Token}s from the {@link ConversationMeta#getLastMessageAnalyzed()} 
     * Message. {@link Token}s with an  {@link Token.Origin#Agent} MUST BE considered independent 
     * from the {@link Token#getMessageIdx()}
     * @param con the conversation
     */
    public void updateTemplates(Conversation con) {
        updateTemplates(con, con.getMeta().getLastMessageAnalyzed());
    }
    /**
     * Builds and updates {@link Template}s for the parsed conversation by
     * considering {@link Token}s from {@link Message}s with in index starting
     * of the parsed <code>startMsgIdx</code>. {@link Token}s with an 
     * {@link Token.Origin#Agent} MUST BE considered independent from the {@link Token#getMessageIdx()}
     * @param con the conversation
     * @param startMsgIdx the start message index for tokens to be considered when building and updating
     * templates
     */
    public void updateTemplates(Conversation con, int startMsgIdx) {
        log.debug("Update QueryTemplates for {} (msgIndx:  {})", con, startMsgIdx);
        final long templStart = System.currentTimeMillis();
        templateBuilders.forEach(builder -> builder.updateTemplate(con, startMsgIdx));
        log.debug("Created QueryTemplates for {} in {}ms", con, System.currentTimeMillis() - templStart);
    }

}
