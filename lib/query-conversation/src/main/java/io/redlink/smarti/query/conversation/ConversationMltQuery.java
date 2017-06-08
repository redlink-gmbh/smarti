/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.State;

/**
 */
public class ConversationMltQuery extends Query {

    private String content;

    public ConversationMltQuery() {
        super();
    }

    public ConversationMltQuery(String creator) {
        super(creator);
    }

    @Override
    public ConversationMltQuery setConfidence(float confidence) {
        super.setConfidence(confidence);
        return this;
    }

    @Override
    public ConversationMltQuery setDisplayTitle(String displayTitle) {
        super.setDisplayTitle(displayTitle);
        return this;
    }

    @Override
    public ConversationMltQuery setUrl(String url) {
        super.setUrl(url);
        return this;
    }

    @Override
    public ConversationMltQuery setCreator(String creator) {
        super.setCreator(creator);
        return this;
    }

    @Override
    public ConversationMltQuery setInlineResultSupport(boolean inlineResultSupport) {
        super.setInlineResultSupport(inlineResultSupport);
        return this;
    }

    @Override
    public ConversationMltQuery setState(State state) {
        super.setState(state);
        return this;
    }

    public ConversationMltQuery setContent(String content) {
        this.content = content;
        return this;
    }

    public String getContent() {
        return content;
    }
}
