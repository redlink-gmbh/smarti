/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.api;

import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

public interface TokenClassifier {

    
    default int classificationOrder(){
        return 0;
    }
    
    /**
     * Classifies the token. Typically classifiers will only
     * process Tokens with the {@link Type#Unknown} as this
     * indicates that this Token was not yet classified. If
     * a classifiers modifies a Token with an other type is
     * will override a classification of an other
     * {@link TokenClassifier}.
     * @param token the token to detect the {@link Type} for
     * @return if the {@link Token} was classified
     */
    boolean classify(Token token);
    
}
