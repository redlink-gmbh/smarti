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

package io.redlink.reisebuddy.extractor.token;

import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Token;

import java.util.List;
import java.util.Set;

/**
 * Implementations of this Interface are expected to assign
 * {@link Hint}s to Tokens mentioned in a {@link Message}.
 * The message is represented by the according {@link Section}
 * in the {@link AnalyzedText}.
 * 
 * @author Rupert Westenthaler
 *
 */
public interface TokenProcessingRuleset {

    /**
     * The language for this rule set
     * @return the language or <code>null</code> for all languages
     */
    String getLanguage();
    
    /**
     * The types of Tokens parsed to this rule set
     * @return
     */
    Set<Token.Type> tokenTypes();
    
    /**
     * The set of {@link MessageTopic}s this rule set applies to
     * @return
     */
    Set<MessageTopic> topics();
    
    /**
     * Applies this rule set to the list of tokens extracted from a{@link Message}
     * @param Message the {@link Message} containing all parsed tokens
     * @param tokens the tokens part of the message, matching the
     * defined {@link #tokenTypes()}
     */
    void apply(Message message, List<Token> tokens);
    
}
