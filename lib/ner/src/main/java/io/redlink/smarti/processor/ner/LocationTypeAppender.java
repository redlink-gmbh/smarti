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
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.redlink.nlp.model.NlpAnnotations.LEMMA_ANNOTATION;
import static io.redlink.nlp.model.NlpAnnotations.NER_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.*;

/**
 * This class looks for {@link NerTag#NAMED_ENTITY_LOCATION} type Named Entities
 * and checks if it can append an other location that follows the first one
 * <p>
 * The intension of this component is to correctly detect Locations
 * like <code>Hamburg Hauptbahnhof</code> or <code>Berlin Hbf.</code> where
 * NER often only marks <code>Hamburg</code> and <code>Berlin</code> as a
 * <code>LOC</code> type named Entity.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class LocationTypeAppender extends Processor {

    private final static Logger log = LoggerFactory.getLogger(LocationTypeAppender.class);
    
    @org.springframework.beans.factory.annotation.Value("${processor.ner.locappend.tags:}")
    private String appendTags; 
    @org.springframework.beans.factory.annotation.Value("${processor.ner.locappend.types:}")
    private String appendTypes; 
    
    //NOTE: we allow LOC and ORG as context as sometimes LOC are marked as ORG if NER misses the 2nd part
    private NerSet contextSet = NerSet.ofType(NerTag.NAMED_ENTITY_LOCATION, NerTag.NAMED_ENTITY_ORGANIZATION);
    //Set in #init() based on #appendTypes and #appendTags
    private NerSet appendSet;
    
    public LocationTypeAppender(){
        super("ner.location.typeappender","Location Type Appender", Phase.extraction,-10); //after NER and before extraction default
    }

    @Override
    public Map<String, Object> getDefaultConfiguration() {
        Map<String,Object> defaultConfig = new HashMap<>();
        defaultConfig.put("processor.ner.locappend.tags", "");
        defaultConfig.put("processor.ner.locappend.types", "");
        return defaultConfig;
    }
    
    
    protected void init(){
        this.appendSet = getAppendSet(appendTypes, appendTags, NerSet.ofType(NerTag.NAMED_ENTITY_LOCATION));
    }

    private NerSet getAppendSet(String appendTypes, String appendTags, NerSet defaultSet) {
        NerSet appendSet;
        if(StringUtils.isNotBlank(appendTags)   || StringUtils.isNotBlank(appendTypes)){
            appendSet = NerSet.empty();
            if(StringUtils.isNotBlank(appendTags)){
                appendSet.addTag(appendTags.split("[, \t]+"));
            }
            if(StringUtils.isNotBlank(appendTypes)){
                appendSet.addTag(appendTypes.split("[, \t]+"));
            }
            log.info(" - set location appender set for parsed NerTags to {}", appendSet);
        } else {
            appendSet = defaultSet;
            log.debug(" - use default location appender set {}",defaultSet);
        }
        return appendSet;
    }

    @Override
    public void doProcessing(ProcessingData processingData) throws ProcessingException {
        Optional<AnalyzedText> ato = NlpUtils.getAnalyzedText(processingData);
        if(!ato.isPresent()){
            return;
        }
        AnalyzedText at = ato.get();
        NerSet appendSet = getAppendSet(processingData.getConfiguration("processor.ner.locappend.types", ""),
                processingData.getConfiguration("processor.ner.locappend.tags", ""), this.appendSet);
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
                        process(section, msgIdx, message, appendSet);
                    }
                }
            } else { //invalid section
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_ANNOTATION.getKey(), 
                        section.getStart(), section.getEnd(), section.getSpan());
            }
        }
    }

    private void process(Section section, int msgIdx, Message message, NerSet appendSet) {
        //wee need to process tokens and chunks
        Iterator<Span> spans = section.getEnclosed(EnumSet.of(SpanTypeEnum.Chunk, SpanTypeEnum.Token));
        log.trace("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        //we might encounter multiple overlapping Named Entities of the same Type.
        //so we use this map to lookup them and build a token covering them all
        List<ChunkState> context = new LinkedList<>();
        while(spans.hasNext()){
            Span span = spans.next();
            boolean processed = false;
            for(Iterator<ChunkState> it = context.iterator(); it.hasNext();){
                ChunkState cs = it.next();
                processed = cs.process(span) || processed;
                if(!cs.active){
                    it.remove();
                }
            }
            if(!processed && span.getType() == SpanTypeEnum.Chunk){
                ChunkState cs = ChunkState.create(Chunk.class.cast(span), contextSet, appendSet);
                if(cs != null){
                    context.add(cs);
                }
            }
        }
    }
    
    /**
     * Internal helper that keeps track of {@link Chunk}s with
     * a {@link NerTag} matching the parsed {@link NerSet}. It
     * allows to process follow up {@link Token}s and {@link Chunk}s
     * so that a {@link #connected} {@link NerTag}s can be identified.
     * In such a case {@link #merge()} can be used to create a 
     * {@link NerTag} covering the original {@link #nerAnno} and the
     * {@link NerTag} of the {@link #connected} {@link Chunk}.
     * @author Rupoert Westenthaler
     *
     */
    private static class ChunkState {
        
        final Value<NerTag> nerAnno;
        final Chunk chunk;
        private final NerSet accepted;
        Chunk connected;
        boolean active = true;
        Chunk merged;
        
        public static ChunkState create(Chunk c, NerSet context, NerSet append) {
            Optional<Value<NerTag>> acceptedNerTag = c.getValues(NER_ANNOTATION).stream()
                    .filter(a -> context.getTypes().contains(a.value().getType()) ||
                            context.getTags().contains(a.value().getTag()))
                    .findFirst();
            if(acceptedNerTag.isPresent() && NlpUtils.isOfNer(c, context)){
                return new ChunkState(c, acceptedNerTag.get(), append);
            } else {
                return null;
            }
        }
        
        private ChunkState(Chunk c, Value<NerTag> nerAnno, NerSet accepted) {
            this.chunk = c;
            this.nerAnno = nerAnno;
            this.accepted = accepted;
        }
        boolean process(Span s){
            switch (s.getType()) {
            case Token:
                return process(Token.class.cast(s));
            case Chunk:
                return process(Chunk.class.cast(s));
            default:
                return active = false;
            }
        }
        private boolean process(Token t){
            if(!active){
                return false;
            }
            return active = t.getEnd() <= chunk.getEnd();
        }
        private boolean process(Chunk c){
            if(!active || connected != null){
                return false; //can not connect more chunks
            }
            if(NlpUtils.isOfNer(c, accepted)){
                NerTag nerTag = new NerTag(NerTag.NAMED_ENTITY_LOCATION, NerTag.NAMED_ENTITY_LOCATION);
                if(c.getStart() >= chunk.getEnd()){ //append this chunk and add a new merged annotation
                    connected = c;
                    merged = merge(nerTag);
                    return true;
                } else if(c.getEnd() == chunk.getEnd() && !NlpUtils.isOfNer(chunk, accepted)){
                    //The appender tails the context, but the contexts NER annotation is not compatible with
                    //the appender ... in this case we want to add an additional NER annotation to the context
                    chunk.addValue(NER_ANNOTATION, Value.value(nerTag, sumProbability(nerAnno.probability(), 
                            NlpUtils.getProbability(c, accepted))));
                    connected = c;
                    String chunkLemma = NlpUtils.getLemma(chunk);
                    String connectedLemma = NlpUtils.getLemma(connected);
                    if(chunkLemma == null && connectedLemma != null){
                        StringBuilder lemma = new StringBuilder();
                        lemma.append(chunk.getContext().getText().subSequence(chunk.getStart(), connected.getStart()));
                        lemma.append(connectedLemma);
                        log.debug("   - lemma: {} (chunk: {})", lemma, chunk);
                        chunk.setAnnotation(LEMMA_ANNOTATION, lemma.toString());
                    }
                    merged = chunk;
                    return true;
                } //else do not accept
            }
            return false;
        }
        
        private Chunk merge(NerTag nerTag){
            if(connected == null){
                log.warn("merge was called for {} - {} with no connected chunk", chunk, nerAnno);
                return null;
            }
            AnalyzedText at = chunk.getContext();
            final double prob = sumProbability(nerAnno.probability(), NlpUtils.getProbability(connected, accepted));
            Chunk mergedChunk = at.addChunk(chunk.getStart(), connected.getEnd());
            Value<NerTag> nerValue = Value.value(nerTag, prob);
            log.debug(" - add Named Entity {} - {}: {}", mergedChunk, mergedChunk.getSpan(), nerValue);
            mergedChunk.addValue(NER_ANNOTATION, nerValue);
            String chunkLemma = NlpUtils.getLemma(chunk);
            String connectedLemma = NlpUtils.getLemma(connected);
            if(chunkLemma != null || connectedLemma != null){
                StringBuilder lemma = new StringBuilder();
                lemma.append(chunkLemma != null ? chunkLemma : chunk.getSpan());
                lemma.append(at.getSpan().substring(chunk.getEnd(), connected.getStart()));
                lemma.append(connectedLemma != null ? connectedLemma : connected.getSpan());
                log.debug("   - lemma: {} (chunk: {}, connected: {})", lemma, chunkLemma, connectedLemma);
                mergedChunk.setAnnotation(LEMMA_ANNOTATION, lemma.toString());
            }
            return mergedChunk;
        }
        
        /**
         * Calculates the probability for an Annotation merged based on the probability of
         * the to original one. Considers {@link Value#UNKNOWN_PROBABILITY} values.
         * @param p1 the probability of the first annotation or {@link Value#UNKNOWN_PROBABILITY} if none
         * @param p2 the probability of the second annotation or {@link Value#UNKNOWN_PROBABILITY} if none
         * @return the probability of the merged annotation
         */
        private double sumProbability(double p1, double p2){
            if(p1 == Value.UNKNOWN_PROBABILITY && p2 == Value.UNKNOWN_PROBABILITY){
                return Value.UNKNOWN_PROBABILITY;
            }
            p1 = p1 == Value.UNKNOWN_PROBABILITY ? 1 : p1;
            p2 = p2 == Value.UNKNOWN_PROBABILITY ? 1 : p2;
            return (p1 + p2)/(1 + (p1*p2));
        }

    }
    
}
