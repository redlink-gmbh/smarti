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

package io.redlink.smarti.processor.ner;


import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.api.model.Value;
import io.redlink.nlp.model.*;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.ner.NerSet;
import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.model.pos.LexicalCategory;
import io.redlink.nlp.model.pos.Pos;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.redlink.smarti.processing.SmartiAnnotations.*;

/**
 * This class looks for {@link NerTag#NAMED_ENTITY_LOCATION} type Named Entities
 * and checks if an {@link LexicalCategory#Adjective} is directly before that
 * Named Entity. If this is the case a new Named Entity including the
 * Adjective is added to AnalyzedText 
 * <p>
 * The intension of this component is to correctly detect Locations
 * like <code>Hamburger Hauptbahnhof</code>, <code>Berliner Innenstadt</code> or
 * <code>Münchner Ostbahnhof</code>
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class AdjectiveLocationProcessor extends Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final PosSet ATTRIB_ADJ = PosSet.of(Pos.AttributiveAdjective);
    
    @org.springframework.beans.factory.annotation.Value("${processor.ner.adjloc.tags:}")
    private String nerTags; 
    
    private NerSet locationSet = NerSet.LOCATION;
    
    public AdjectiveLocationProcessor(){
        super("ner.adjectivlocation","Adjective Location Processor",Phase.extraction,
                -10); //after NER and before extraction default
    }

    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.singletonMap("processor.ner.adjloc.tags", "");
    }
    
    protected void init(){
        this.locationSet = getLocationSet(nerTags, NerSet.LOCATION);
    }
    
    @Override
    protected void doProcessing(ProcessingData processingData) throws ProcessingException {
        Optional<AnalyzedText> ato = NlpUtils.getAnalyzedText(processingData);
        if(!ato.isPresent()){
            return; //Nothing to do
        }
        
        final NerSet locationSet = getLocationSet(processingData.getConfiguration("processor.ner.adjloc.tags", ""), this.locationSet);
        AnalyzedText at = ato.get();
        Conversation conv = processingData.getAnnotation(CONVERSATION_ANNOTATION);
        if(conv == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, CONVERSATION_ANNOTATION);
            return;
        }
        List<Message> messages = conv.getMessages();

        //NOTE: startMsgIdx was used in the old API to tell TemplateBuilders where to start. As this might get (re)-
        //      added in the future (however in a different form) we set it to the default 0 (start from the beginning)
        //      to keep the code for now
        int lastAnalyzed = -1;
        
        Iterator<Section> sections = at.getSections();
        while(sections.hasNext()){
            Section section = sections.next();
            Integer msgIdx = section.getAnnotation(MESSAGE_IDX_ANNOTATION);
            if(msgIdx != null){
                if(msgIdx > lastAnalyzed && msgIdx < messages.size()){
                    Message message = messages.get(msgIdx);
                    if(Origin.User == message.getOrigin()){
                        process(section, msgIdx, message, locationSet);
                    }
                }
            } else { //invalid section
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_ANNOTATION.getKey(), 
                        section.getStart(), section.getEnd(), section.getSpan());
            }
        }
    }

    private NerSet getLocationSet(String nerTags, NerSet defaultNerSet) {
        final NerSet locationSet;
        if(StringUtils.isNotBlank(nerTags)){
            locationSet = NerSet.ofTag(nerTags.split("[, \t]+"));
            log.trace(" - set location set for parsed NerTags to {}", locationSet);
        } else {
            locationSet = defaultNerSet;
            log.trace(" - use default location set {}", locationSet);
        }
        return locationSet;
    }

    private void process(Section section, int msgIdx, Message message, NerSet locationSet) {
        AnalyzedText at = section.getContext();
        //wee need to process tokens and chunks
        Iterator<Span> spans = section.getEnclosed(EnumSet.of(SpanTypeEnum.Token, SpanTypeEnum.Chunk));
        log.trace("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        //we might encounter multiple overlapping Named Entities of the same Type.
        //so we use this map to lookup them and build a token covering them all
        Token adjectiveContext = null;
        int endLocation = -1;
        while(spans.hasNext()){
            Span span = spans.next();
            switch (span.getType()) {
            case Token:
                Token token = (Token)span;
                if(endLocation < token.getEnd() && NlpUtils.isOfPos(token, ATTRIB_ADJ)){
                    adjectiveContext = token;
                } else {
                    adjectiveContext = null;
                }
                break;
            case Chunk:
                Chunk chunk = (Chunk)span;
                if(endLocation < chunk.getEnd() && NlpUtils.isOfNer(chunk, locationSet)){
                    endLocation = chunk.getEnd();
                    if(adjectiveContext != null){
                        Value<NerTag> nerAnno = chunk.getValue(NlpAnnotations.NER_ANNOTATION);
                        double posProb = NlpUtils.getProbability(adjectiveContext, ATTRIB_ADJ);
                        double nerPorb = NlpUtils.getProbability(chunk, locationSet);
                        Chunk nerChunk = at.addChunk(adjectiveContext.getStart(), chunk.getEnd());
                        Value<NerTag> nerValue = Value.value(new NerTag(nerAnno.value().getTag(),NerTag.NAMED_ENTITY_LOCATION), 
                                sumProbability(posProb, nerPorb));
                        log.debug(" - add Named Entity {} - {}: {}", chunk, chunk.getSpan(), nerValue);
                        nerChunk.addValue(NlpAnnotations.NER_ANNOTATION, nerValue);
                        String adjLemma = NlpUtils.getLemma(adjectiveContext);
                        String nerLemma = NlpUtils.getLemma(chunk);
                        if(adjLemma != null || nerLemma != null){
                            StringBuilder lemma = new StringBuilder();
                            lemma.append(adjLemma != null ? adjLemma : adjectiveContext.getSpan());
                            lemma.append(at.getSpan().substring(adjectiveContext.getEnd(), chunk.getStart()));
                            lemma.append(nerLemma != null ? nerLemma : chunk.getSpan());
                            log.debug("   - lemma: {} (adjLemma: {}, nerLemma: {})", lemma, adjLemma, nerLemma);
                            nerChunk.setValue(NlpAnnotations.LEMMA_ANNOTATION, Value.value(lemma.toString()));
                        }
                        adjectiveContext = null;
                    }
                }
            default:
                break;
            }
        }
    }
    
    private double sumProbability(double prop1, double prob2){
        return (prop1 + prob2)/(1 + (prop1*prob2));
    }

}
