/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.reisebuddy.extractor.token;

import java.util.List;
import java.util.Set;

import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Token;

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
