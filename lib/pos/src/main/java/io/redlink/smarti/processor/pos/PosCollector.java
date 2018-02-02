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
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.pos.Pos;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.model.Token.Type;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;

import static io.redlink.smarti.processing.SmartiAnnotations.ANALYSIS_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;

/**
 * Allows to create Tokens for Words with specific Part-of-Speech (POS) tags. By default this component is configured 
 * to create Tokens with the type `Attribute` for words that are classified as adjectives.
 * 
 * A stopword list with words that are ignored is provided in <code>wordlist/ignored_adjectives.txt</code>
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class PosCollector extends Processor {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String IGNORED_RESOURCE = "wordlist/ignored_adjectives.txt";
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * All tokens indicating a new section within an sentence
     */
    public static final PosSet MEDIAL_PUNCTATION = PosSet.of(Pos.SentenceMedialPunctuation);

    private Set<String> ignoredAdjectives = new HashSet<>();
    
    public PosCollector(){
        super("pos.poscollector", "POS Collector", Phase.extraction);
    }
    
    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap(); //TODO configuration
    }
    
    protected void init(){
        try {
            ignoredAdjectives = loadWords();
        } catch (IOException e){
            log.warn("Unable to load wordlist with ignored adjectives from resource: '{}'", IGNORED_RESOURCE);
            log.debug("EXCEPTION", e);
        }
    }

    private Set<String> loadWords() throws IOException {
        Set<String> words = new HashSet<>();
        
        try (Reader r = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(IGNORED_RESOURCE),UTF8)){
            Iterator<String> lines = IOUtils.lineIterator(r);
            while(lines.hasNext()){
                String word = StringUtils.trimToNull(lines.next());
                if(word != null){
                    words.add(word.toLowerCase(Locale.ROOT));
                }
            }
        } catch (NullPointerException e) {
            log.warn("Wordlist for ignored adjectives (resource: {}) not present", IGNORED_RESOURCE);
            log.debug("EXCEPTION", e);
        }
        return words;
    }

    
    @Override
    public void doProcessing(ProcessingData processingData) {
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
        Analysis analysis = processingData.getAnnotation(ANALYSIS_ANNOTATION);
        if(analysis == null){
            log.warn("parsed {} does not have a '{}' annotation", processingData, ANALYSIS_ANNOTATION);
            return;
        }
        
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
        //We look also at negated Chunks and mark attributes extracted form those as negated
        Iterator<io.redlink.nlp.model.Token> words = section.getTokens();
        log.trace("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        List<Token> tokens = new ArrayList<>();
        while(words.hasNext()){
            io.redlink.nlp.model.Token word = words.next();
            if(log.isTraceEnabled()){
                log.trace("{}: {}", word, word.getAnnotations(NlpAnnotations.POS_ANNOTATION));
            }
            //only adjectives that are no stop words and have only alphabetic chars
            if(NlpUtils.isAdjective(word) && !NlpUtils.isStopword(word) && 
                    NlpUtils.isAlpha(word) && //NOTE: isAlpha accepts also hyphens and underlines
                    !ignoredAdjectives.contains(word.getSpan().toLowerCase(Locale.ROOT))){ 
                Token token = new Token();
                token.setMessageIdx(msgIdx);
                token.setStart(word.getStart() - section.getStart());
                token.setEnd(word.getEnd() - section.getStart());
                token.setValue(word.getSpan());
                token.setType(Type.Attribute);
                token.setConfidence((float)NlpUtils.getProbability(word, PosSet.ADJECTIVES));
                log.debug(" - Attribute [idx:{}, start:{}, end:{}] '{}'{}",
                        token.getMessageIdx(),
                        token.getStart(), token.getEnd(), token.getValue(),
                        token.hasHint(Hint.negated) ? " (negated)" : "");
                tokens.add(token);
            }
        }
        return tokens;
    }

}
