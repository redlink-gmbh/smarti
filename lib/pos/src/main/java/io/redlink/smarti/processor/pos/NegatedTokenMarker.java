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

package io.redlink.smarti.processor.pos;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Chunk;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.processing.SmartiAnnotations;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.redlink.nlp.model.NlpAnnotations.NEGATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.ANALYSIS_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;

@Component
public class NegatedTokenMarker extends Processor {

    
    public NegatedTokenMarker() {
        super("negation.tokenmarker","Negated Token Marker",Phase.post,100); //POST
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
    protected void doProcessing(ProcessingData processingData) throws ProcessingException {
        Optional<AnalyzedText> ato = NlpUtils.getAnalyzedText(processingData);
        if(!ato.isPresent()){
            return; //nothing to do
        }
        AnalyzedText at = ato.get();
        Analysis analysis = processingData.getAnnotation(ANALYSIS_ANNOTATION);
        if(analysis == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, ANALYSIS_ANNOTATION);
            return;
        }
        //NOTE: startMsgIdx was used in the old API to tell TemplateBuilders where to start. As this might get (re)-
        //      added in the future (however in a different form) we set it to the default 0 (start from the beginning)
        //      to keep the code for now
        int startIdx = 0;

        Iterator<Section> sections = at.getSections();
        while(sections.hasNext()){
            Section section = sections.next();
            Integer msgIdx = section.getAnnotation(MESSAGE_IDX_ANNOTATION);
            if(msgIdx != null && msgIdx >= startIdx ){
                List<Token> msgTokens = analysis.getTokens().stream()
                        .filter(t -> t.getMessageIdx() == msgIdx)
                        .collect(Collectors.toList());
                if(!msgTokens.isEmpty()){
                    int offset = section.getStart();
                    Iterator<Chunk> chunks = section.getChunks();
                    while(chunks.hasNext()){
                        Chunk chunk = chunks.next();
                        Boolean negation = chunk.getAnnotation(NEGATION_ANNOTATION);
                        if(Boolean.TRUE.equals(negation)){
                            int negStart = chunk.getStart() - offset;
                            int negEnd = chunk.getEnd() - offset;
                            for(Token token : msgTokens){
                                if(token.getStart() >= negStart && token.getEnd() <= negEnd){
                                    token.addHint(Hint.negated);
                                    log.debug("  - negate {}", token);
                                }
                            }
                        }
                    }
                }
                
                
            }
        }

    }

}
