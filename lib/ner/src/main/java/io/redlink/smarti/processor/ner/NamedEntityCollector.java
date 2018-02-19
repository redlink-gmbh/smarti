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
import io.redlink.nlp.model.ner.NerTag;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.model.pos.PosTag;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.redlink.nlp.model.NlpAnnotations.NER_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.ANALYSIS_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;

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
        m.put(NerTag.NAMED_ENTITY_PERSON, Token.Type.Person);
        m.put(NerTag.NAMED_ENTITY_ORGANIZATION, Token.Type.Organization);
        //m.put(NerTag.NAMED_ENTITY_EVENT, Token.Type.Event);
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
        Analysis analysis = processingData.getAnnotation(ANALYSIS_ANNOTATION);
        if(analysis == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, ANALYSIS_ANNOTATION);
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
                        analysis.getTokens().addAll(createNamedEntityTokens(section, msgIdx, message));
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
        log.trace("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
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
                        log.warn("{} contains alpha numeric spans without POS tags. Will mark all NER annotations as valid", word);
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
                        addTypeHint(token, nerTag);
                    } else { //merge existing token
                        if(end > token.getEnd()){
                            token.setEnd(end); //update end and value
                            token.setValue(section.getSpan().substring(token.getStart(), end));
                        }
                        //also update the confidence
                        //TODO: when merging tokens Lemmas are not supported
                        token.setConfidence(sumProbability(token.getConfidence(), getProbability(nerAnno)));
                        addTypeHint(token, nerTag);
                    }
                    activeTokens.put(type, token); //put the active token to the map
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
     * @return the mapped Token {@link Type} or {@link Type#Entity} if no specific mapping was present
     */
    private Type getTokenType(NerTag nerTag) {
        Token.Type type = TOKEN_TYPE_MAPPINGS.get(nerTag.getType()); //(1) try direct mappings
        if(type == null && nerTag.getType() != null){
            try { //(2) try to match the type against the Token.Type enumeration
                type = Token.Type.valueOf(nerTag.getType());
            } catch (IllegalArgumentException e) { /* tag is not a Token.Type */}
        }
        if(type == null && nerTag.getTag() != null &&
                (nerTag.getType() == null || NerTag.NAMED_ENTITY_UNKOWN.equals(nerTag.getType()) || NerTag.NAMED_ENTITY_MISC.equals(nerTag.getType()))){
            type = TOKEN_TYPE_MAPPINGS.get(nerTag.getTag()); //(3) try direct mappings for tag
            if(type == null){
                try { //(4) try to match the tag against the Token.Type enumeration
                    type = Token.Type.valueOf(nerTag.getTag());
                } catch (IllegalArgumentException e) { /* tag is not a Token.Type */}
            }
        }
        return type == null ? Token.Type.Entity : type;
    }
    
    private void addTypeHint(Token token, NerTag nerTag){
        if(StringUtils.isBlank(nerTag.getType()) || token.getType() != Token.Type.Entity){
            if(StringUtils.isNoneBlank(nerTag.getTag())){
                token.addHint(String.format("entity.type.%s",nerTag.getType()));
            }
        } else { //use the type 
            token.addHint(String.format("entity.type.%s",nerTag.getTag()));
        }
    }

}
