/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.processor.ner;

import static io.redlink.nlp.model.NlpAnnotations.NER_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.api.model.Value;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Chunk;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.processing.SmartiAnnotations;

/**
 * This class collects Named Entity Annotations created (by possible
 * multiple NER components) in the {@link AnalyzedText} and creates 
 * {@link Token}s in the {@link Conversation} for those contained
 * in {@link Message}s with {@link Origin#User} and an index greater
 * as {@link ConversationMeta#getLastMessageAnalyzed()}
 * <p>
 * This {@link QueryPreparator} DOES NOT extract Named Entities by itself!
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class NamedEntityCollector extends Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final float DEFAULT_PROB = 0.8f;
    
    private static final Map<String,Token.Type> TOKEN_TYPE_MAPPINGS;
    static {
        Map<String,Token.Type> m = new HashMap<>();
        m.put(NerTag.NAMED_ENTITY_LOCATION, Token.Type.Place);
        TOKEN_TYPE_MAPPINGS = Collections.unmodifiableMap(m);
    }
    
    public NamedEntityCollector(){
        super("token.ner","Named Entity Token Collector", Phase.extraction);
    }
    
    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap();
    }

    @Override
    protected void init() throws Exception {
        //No op
    }
    
    @Override
    public void doProcessing(ProcessingData processingData) throws ProcessingException {
        Optional<AnalyzedText> ato = NlpUtils.getAnalyzedText(processingData);
        if(!ato.isPresent()){
            return; //nothing to do
        }
        AnalyzedText at = ato.get();
        Conversation conv = processingData.getAnnotation(CONVERSATION_ANNOTATION);
        if(conv == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, CONVERSATION_ANNOTATION);
            return;
        }
        List<Message> messages = conv.getMessages();
        int lastAnalyzed = conv.getMeta().getLastMessageAnalyzed();
        Iterator<Section> sections = at.getSections();
        while(sections.hasNext()){
            Section section = sections.next();
            Integer msgIdx = section.getAnnotation(MESSAGE_IDX_ANNOTATION);
            if(msgIdx != null){
                if(msgIdx > lastAnalyzed && msgIdx < messages.size()){
                    Message message = messages.get(msgIdx);
                    if(Origin.User == message.getOrigin()){
                        conv.getTokens().addAll(createNamedEntityTokens(section, msgIdx, message));
                    }
                }
            } else { //invalid section
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_IDX_ANNOTATION, 
                        section.getStart(), section.getEnd(), section.getSpan());
            }
        }
    }

    private List<Token> createNamedEntityTokens(Section section, int msgIdx, Message message) {
        Iterator<Chunk> chunks = section.getChunks();
        log.debug("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        //we might encounter multiple overlapping Named Entities of the same Type.
        //so we use this map to lookup them and build a token covering them all
        Map<Token.Type, Token> activeTokens = new EnumMap<>(Token.Type.class);
        List<Token> tokens = new ArrayList<>();
        while(chunks.hasNext()){
            Chunk chunk = chunks.next();
            int start = chunk.getStart()-section.getStart();
            int end = chunk.getEnd()-section.getStart();
            String lemma = NlpUtils.getLemma(chunk);
            if(lemma == null){
                lemma = chunk.getSpan();
            }
            List<Value<NerTag>> nerAnnotations = chunk.getValues(NER_ANNOTATION);
            for(Value<NerTag> nerAnno : nerAnnotations){
                NerTag nerTag = nerAnno.value();
                Token.Type type = getTokenType(nerTag);
                if(type != null){
                    log.debug(" - [{},{}] {} (tag:{} | lemma: {}) - type: {}",start, end, chunk.getSpan(), nerTag.getType(), lemma, type);
                    Token token = activeTokens.get(type);
                    if(token == null || token.getEnd() <= start){
                        if(token != null){
                            tokens.add(token);
                        }
                        token = new Token();
                        token.setMessageIdx(msgIdx);
                        token.setStart(start);
                        token.setEnd(end);
                        token.setType(type);
                        token.setValue(lemma);
                        token.setConfidence(getProbability(nerAnno));
                        activeTokens.put(type, token);
                    } else { //merge existing token
                        if(end > token.getEnd()){
                            token.setEnd(end); //update end and value
                            token.setValue(section.getSpan().substring(token.getStart(), end));
                        }
                        //also update the confidence
                        //TODO: when merging tokens Lemmas are not supported
                        token.setConfidence(sumProbability(token.getConfidence(), getProbability(nerAnno)));
                    }
                } else {
                    log.trace(" - [{},{}] {} (tag:{}) - unmapped", start, end, chunk.getSpan(), nerTag.getType());
                }
            }
        }
        tokens.addAll(activeTokens.values());
        Collections.sort(tokens, Token.IDX_START_END_COMPARATOR);
        return tokens;
    }

    private float getProbability(Value<?> nerAnno) {
        if(nerAnno.probability() == Value.UNKNOWN_PROBABILITY){
            return DEFAULT_PROB;
        } else {
            return (float)nerAnno.probability();
        }
    }
    private float sumProbability(float prop1, float prob2){
        return (prop1 + prob2)/(1 + (prop1*prob2));
    }

    private Type getTokenType(NerTag nerTag) {
        Token.Type type = TOKEN_TYPE_MAPPINGS.get(nerTag.getType());
        if(type == null){
            type = TOKEN_TYPE_MAPPINGS.get(nerTag.getTag());
        }
        return type;
    }

}
