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
