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
