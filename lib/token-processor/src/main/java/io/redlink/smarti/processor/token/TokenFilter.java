package io.redlink.smarti.processor.token;

import org.springframework.stereotype.Component;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.processor.token.impl.TokenFilterProcessor;

/**
 * {@link Component}s implementing this interface will be used
 * by the {@link TokenFilterProcessor} to filter {@link Token}s 
 * extracted from {@link Message}s of a {@link Conversation}
 * an conversation
 * @author Rupert Westenthaler
 *
 */
public interface TokenFilter {

    /**
     * Checks if the parsed token should be filtered given the
     * parsed {@link Message} and {@link Conversation} as context
     * @param token the token
     * @param lang the language of the conversation
     * @param con the conversation
     * @return <code>true</code> if the token should be removed.
     * Otherwise <code>false</code>
     */
    boolean filter(Token token, String lang, Conversation con);
    
}
