/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.reisebuddy.extractor.token.impl;

import io.redlink.reisebuddy.api.QueryPreparator;
import io.redlink.reisebuddy.model.Conversation;
import io.redlink.reisebuddy.model.Token;
import io.redlink.reisebuddy.model.Token.Hint;
import io.redlink.reisebuddy.processing.ProcessingData;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * {@link QueryPreparator} that filters overlapping Tokens of
 * the same type. This happens if multiple QueryPreparators
 * do mark the same mention in the conversation as an Token with
 * the same {@link Token.Type}.
 * <p>
 * {@link Hint}s of filtered {@link Token}s are copied over to the
 * token that is kept.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class TokenFilter extends QueryPreparator {

    protected TokenFilter() {
        super(Phase.post,10); //the last one
    }

    @Override
    public void prepare(ProcessingData processingData) {
        Conversation c = processingData.getConversation();
        //(1) we need to find the first token created by this analysis step
        int lastAnalyzed = c.getMeta().getLastMessageAnalyzed();
        final List<Token> newTokens;
        if(lastAnalyzed >= 0){ //we need to find the first new token
            List<Token> tokens = c.getTokens();
            int firstNewTokenIdx = 0;
            for(int i = tokens.size() - 1; i >= 0; i--){
                Token token = tokens.get(i);
                if(token.getMessageIdx() <= lastAnalyzed){
                    firstNewTokenIdx = i + 1;
                    break;
                }
            }
            newTokens = tokens.subList(firstNewTokenIdx, tokens.size());
        } else { //all tokens are new
            newTokens = c.getTokens();
        }
        //(2) sort the new tokens
        Collections.sort(newTokens, Token.IDX_START_END_COMPARATOR);
        //(3) filter for tokens contained in an other token with the same type
        Map<Token.Type, Token> activeTokens = new EnumMap<>(Token.Type.class);
        for(Iterator<Token> it = newTokens.iterator(); it.hasNext();){
            Token token = it.next();
            Token active = activeTokens.get(token.getType());
            if(active == null || active.getMessageIdx() != token.getMessageIdx() ||
                    token.getEnd() > active.getEnd() || !token.getValue().equals(active.getValue())){
                activeTokens.put(token.getType(), token);
            } else {
                log.debug("filter token {} contained in {}", token, active);
                it.remove();
                for(String hint : token.getHints()){
                    if(active.addHint(hint)){
                        log.debug("  ... copied Hint {} from contained Token", hint);
                    }
                }
            }
        }
    }

}
