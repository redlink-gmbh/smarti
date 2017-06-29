package io.redlink.smarti.processor.keyword.intrestingterms;

import io.redlink.nlp.api.model.Annotation;

public interface InterestingTermsConst {
    
    /**
     * Annotation internally used to store interesting terms. The {@link Value#value()} is the
     * string key of the term. The {@link Value#value()} is used for the importance of
     * the term relative to others marked in the same document.
     */
    public final static Annotation<InterestingTerm> INTERESTING_TERM = new Annotation<>("io_redlink_keyword_interestingTerm", InterestingTerm.class);

}
