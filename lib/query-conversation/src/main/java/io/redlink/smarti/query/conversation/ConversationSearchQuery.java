/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.redlink.smarti.model.Query;

/**
 */
public class ConversationSearchQuery extends Query {

    private List<String> keywords = new ArrayList<>();

    public ConversationSearchQuery() {
        super();
    }

    public ConversationSearchQuery(String creator) {
        super(creator);
    }

    public void setKeyword(String keyword) {
        this.keywords.clear();
        this.keywords.add(keyword);
    }

    public List<String> getKeyword() {
        return Collections.unmodifiableList(keywords);
    }

    public void setKeywords(Collection<String> strings) {
        keywords.clear();
        keywords.addAll(strings);
    }
}
