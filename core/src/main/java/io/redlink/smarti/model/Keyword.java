/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.model;

import org.springframework.data.annotation.PersistenceConstructor;

/**
 * Provides all information about an extracted keyword needed to write the
 * KeywordAnnotation
 * 
 * TODO: check of {@link #getContained()} is a good Idea, or if we should use
 * a flat list of keywords with references to contained one instead
 * 
 * @author Rupert Westenthaler
 * @deprecated directly use {@link io.redlink.nlp.model.keyword.Keyword}
 */
public class Keyword extends io.redlink.nlp.model.keyword.Keyword{
    
    @PersistenceConstructor
    public Keyword(String key, String keyword) {
        super(key, keyword);
    }
    

    
}