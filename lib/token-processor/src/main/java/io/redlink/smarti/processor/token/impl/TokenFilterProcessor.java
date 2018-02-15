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

package io.redlink.smarti.processor.token.impl;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.Processor;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.processor.token.TokenFilter;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.redlink.smarti.processing.SmartiAnnotations.ANALYSIS_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;

/**
 * {@link QueryPreparator} that merges overlapping Tokens of
 * the same type. This happens if multiple QueryPreparators
 * do mark the same mention in the conversation as an Token with
 * the same {@link Token.Type}.
 * <p>
 * {@link Hint}s of merged {@link Token}s are copied over to the
 * token that is kept.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class TokenFilterProcessor extends Processor {

    private List<TokenFilter> tokenFilters;

    protected TokenFilterProcessor(List<TokenFilter> tokenFilters) {
        super("token.filter","Token Filter",Phase.post,1); //after the first post processing processors
        this.tokenFilters = tokenFilters;
    }

    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap();
    }
    
    @Override
    protected void init() throws Exception {
        //no op
    }
    
    @Override
    public void doProcessing(ProcessingData processingData) {
        if(CollectionUtils.isEmpty(tokenFilters)){
            return; //no filters available
        }
        Conversation c = processingData.getAnnotation(CONVERSATION_ANNOTATION);
        if(c == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, CONVERSATION_ANNOTATION);
            return;
        }
        Analysis analysis = processingData.getAnnotation(ANALYSIS_ANNOTATION);
        if(analysis == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, ANALYSIS_ANNOTATION);
            return;
        }
        String lang = processingData.getLanguage();

        //NOTE: startMsgIdx was used in the old API to tell TemplateBuilders where to start. As this might get (re)-
        //      added in the future (however in a different form) we set it to the default 0 (start from the beginning)
        //      to keep the code for now
        int lastAnalyzed = -1;
        
        final List<Token> newTokens;
        if(lastAnalyzed >= 0){ //we need to find the first new token
            List<Token> tokens = analysis.getTokens();
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
            newTokens = analysis.getTokens();
        }
        //filter the tokens
        for(Iterator<Token> it = newTokens.iterator();it.hasNext();){
            Token token = it.next();
            if(tokenFilters.stream().filter(tf -> tf.filter(token, lang, c)).findAny().isPresent()){
                it.remove(); //found a TokenFilter that wants to filter this token ... so remove it
            }
        }
    }

}
