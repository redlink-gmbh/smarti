/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.dbsearch;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import io.redlink.smarti.model.Query;

/**
 */
public class DbSearchQuery extends Query {

    private Set<String> fullTextTerms = new LinkedHashSet<>();

    public DbSearchQuery() {
        super();
    }

    public DbSearchQuery(String creator) {
        super(creator);
    }

    public void setTerm(String keyword) {
        this.fullTextTerms.clear();
        this.fullTextTerms.add(keyword);
    }
    public void addTerm(String term){
        this.fullTextTerms.add(term);
    }

    public Collection<String> getTerms() {
        return fullTextTerms;
    }

    public void setTerms(Collection<String> strings) {
        fullTextTerms.clear();
        fullTextTerms.addAll(strings);
    }
}

