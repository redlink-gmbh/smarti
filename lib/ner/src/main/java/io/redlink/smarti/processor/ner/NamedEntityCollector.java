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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import io.redlink.nlp.model.Span;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.model.pos.PosTag;
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
    
    /**
     * {@link Token#addHint(String) Hint} used to makr Named Entities that cover an to
     */
    private static final String HINT_INTERSTING_NAMED_ENTITY = "_internal.interstingWordNamedEntity";

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
        Iterator<Span> spans = section.getEnclosed(EnumSet.of(SpanTypeEnum.Token,SpanTypeEnum.Chunk));
        log.debug("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        //we might encounter multiple overlapping Named Entities of the same Type.
        //so we use this map to lookup them and build a token covering them all
        Map<Token.Type, Token> activeTokens = new EnumMap<>(Token.Type.class);
        List<Token> tokens = new ArrayList<>();
        PosSet interesting = PosSet.union(PosSet.NOUNS,PosSet.ADJECTIVES);
        boolean loggedNoPosTagsWarning = false;
        while(spans.hasNext()){
            Span span = spans.next();
            int start = span.getStart()-section.getStart();
            int end = span.getEnd()-section.getStart();
            switch(span.getType()){
            case Token: //for tokens we need to look for POS annotations that indicate a valid token for Named Entities
                io.redlink.nlp.model.Token word = (io.redlink.nlp.model.Token)span;
                List<Value<PosTag>> posAnnotations = span.getValues(NlpAnnotations.POS_ANNOTATION);
                if(!posAnnotations.isEmpty()){
                    if(NlpUtils.isOfPos(word, interesting)){
                        //mark all Tokens that cover this word as an interesting named entity
                        activeTokens.values().stream()
                        .filter(t -> t.getEnd() >= end) //the token needs to cover this word
                        .forEach(t -> t.addHint(HINT_INTERSTING_NAMED_ENTITY));
                    }
                } else if(NlpUtils.hasAlphaNumeric(word)){ //no POS Tags?
                    if(!loggedNoPosTagsWarning){
                        log.warn("{} contains alpha numeric spans without POS tags. Will mark all NER annotations as valid");
                        loggedNoPosTagsWarning = true;
                    }
                    //mark all Tokens that cover this word as an interesting named entity
                    activeTokens.values().stream()
                        .filter(t -> t.getEnd() >= end) //the token needs to cover this word
                        .forEach(t -> t.addHint(HINT_INTERSTING_NAMED_ENTITY));
                }
                break;
            case Chunk: //for chunks we need to look for NER annotations
                Chunk chunk = (Chunk)span;
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
                        Token token = activeTokens.remove(type); //consume active tokens
                        if(token == null || token.getEnd() <= start){
                            if(token != null && token.removeHint(HINT_INTERSTING_NAMED_ENTITY)){
                                tokens.add(token);
                            }
                            token = new Token();
                            token.setMessageIdx(msgIdx);
                            token.setStart(start);
                            token.setEnd(end);
                            token.setType(type);
                            token.setValue(lemma);
                            token.setConfidence(getProbability(nerAnno));
                        } else { //merge existing token
                            if(end > token.getEnd()){
                                token.setEnd(end); //update end and value
                                token.setValue(section.getSpan().substring(token.getStart(), end));
                            }
                            //also update the confidence
                            //TODO: when merging tokens Lemmas are not supported
                            token.setConfidence(sumProbability(token.getConfidence(), getProbability(nerAnno)));
                        }
                        activeTokens.put(type, token); //put the active token to the map
                    } else {
                        log.warn("Unable to map NerTag[{},{}] {} (tag:{}) to a Token.Type", start, end, chunk.getSpan(), nerTag.getType());
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected Span with type "+span.getType());
            }
        }
        activeTokens.values().stream()
            .filter(t -> t.removeHint(HINT_INTERSTING_NAMED_ENTITY)) //only those that are interesting
            .collect(Collectors.toCollection(() -> tokens)); //are added to the tokens
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
    /**
     * Tries to map the {@link NerTag} to a Token {@link Type}
     * @param nerTag the nerTag
     * @return the TokenType
     */
    private Type getTokenType(NerTag nerTag) {
        Token.Type type = TOKEN_TYPE_MAPPINGS.get(nerTag.getType());
        if(type == null){
            if(nerTag.getType().equals(NerTag.NAMED_ENTITY_MISC) || nerTag.getType().equals(NerTag.NAMED_ENTITY_UNKOWN)){
                type = TOKEN_TYPE_MAPPINGS.get(nerTag.getTag());
                if(type == null){
                    try {
                        type = Token.Type.valueOf(nerTag.getTag());
                    } catch (IllegalArgumentException e) { /* tag is not a Token.Type */}
                }
            } else {
                try {
                    type = Token.Type.valueOf(nerTag.getTag());
                } catch (IllegalArgumentException e) { /* tag is not a Token.Type */}
            }
        }
        return type;
    }

}
