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
import io.redlink.nlp.model.*;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.phrase.PhraseCategory;
import io.redlink.nlp.model.phrase.PhraseTag;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
//@Component
public class PhraseCollector extends Processor {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Set<PhraseCategory> NOUN_PHRASE_CATEGORIES = EnumSet.of(
            PhraseCategory.NounPhrase,PhraseCategory.NounHeadedPhrase,PhraseCategory.ForeignPhrase);
    
    public PhraseCollector(){
        super("pos.phrasecollector", "Phrase Collector", Phase.extraction);
    }
    
    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap();
    }
    
    @Override
    protected void init() throws Exception {
        //currenty no op
    }
    
    @Override
    public void doProcessing(ProcessingData processingData) throws ProcessingException {
        Optional<AnalyzedText> ato = NlpUtils.getAnalyzedText(processingData);
        if(!ato.isPresent()){
            return; //nothing to do
        }
        AnalyzedText at = ato.get();
        Conversation conv = at.getAnnotation(CONVERSATION_ANNOTATION);
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
                        analysis.getTokens().addAll(createPhraseTokens(section, msgIdx, message));
                    }
                }
            } else { //invalid section
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_IDX_ANNOTATION, 
                        section.getStart(), section.getEnd(), section.getSpan());
            }
        }
    }

    private List<Token> createPhraseTokens(Section section, int msgIdx, Message message) {
        //We look also at negated Chunks and mark attributes extracted form those as negated
        Iterator<Span> spans = section.getEnclosed(EnumSet.of(SpanTypeEnum.Token, SpanTypeEnum.Chunk));
        log.debug("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        List<Token> tokens = new ArrayList<>();
        PhraseTokenData contextPhrase = null;
        while(spans.hasNext()){
            Span span = spans.next();
            switch(span.getType()){
            case Chunk:
                Chunk phrase = (Chunk)span;
                PhraseTag phraseAnno = phrase.getAnnotation(NlpAnnotations.PHRASE_ANNOTATION);
                if(phraseAnno != null && //this chunk has a phrase annotation and it is a noun phrase
                        !Collections.disjoint(NOUN_PHRASE_CATEGORIES,phraseAnno.getCategories())){
                    if(contextPhrase != null){
                        if(phrase.getStart() > contextPhrase.end){
                            tokens.add(createToken(section, msgIdx, contextPhrase));
                            contextPhrase = null;
                        } else {
                            contextPhrase.append(phrase, phraseAnno);
                        }
                    }
                    if(contextPhrase == null){
                        contextPhrase = new PhraseTokenData(phrase, phraseAnno);
                    }
                }
                break;
            case Token:
                io.redlink.nlp.model.Token word = (io.redlink.nlp.model.Token)span;
                if(contextPhrase != null && NlpUtils.isNoun(word) && 
                        contextPhrase.end >= word.getEnd()){
                    contextPhrase.addNoun(word);
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
        if(contextPhrase != null){
            tokens.add(createToken(section, msgIdx, contextPhrase));
        }
        return tokens;
    }

    private Token createToken(Section section, int msgIdx, PhraseTokenData contextPhrase) {
        Token token = new Token();
        token.setMessageIdx(msgIdx);
        token.setStart(contextPhrase.start - section.getStart());
        token.setEnd(contextPhrase.end - section.getStart());
        token.setValue(section.getSpan().substring(token.getStart(), token.getEnd()));
        token.setType(Type.Keyword);
        token.addHint("nounphrase");
        return token;
    }

    class PhraseTokenData {
        
        final int start;
        int end;
        
        final List<PhraseTag> tags = new LinkedList<>();
        final List<io.redlink.nlp.model.Token> nouns = new LinkedList<>();
        
        
        public PhraseTokenData(Chunk phrase, PhraseTag tag) {
            assert phrase != null;
            assert tag != null;
            this.start = phrase.getStart();
            this.end = phrase.getEnd();
            tags.add(tag);
        }

        public void append(Chunk phrase, PhraseTag tag) {
            assert phrase != null;
            assert tag != null;
            assert phrase.getStart() <= end;
            if(phrase.getEnd() > end){
                this.end = phrase.getEnd();
            }
            this.tags.add(tag);
        }
        
        public void addNoun(io.redlink.nlp.model.Token token) {
            assert token != null;
            assert token.getStart() >= start;
            assert token.getEnd() <= end;
            nouns.add(token);
        }
    }
    
}
