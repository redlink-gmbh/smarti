/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
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
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Hint;
import io.redlink.smarti.model.Token.Type;
import io.redlink.smarti.processing.SmartiAnnotations;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;

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
        //We look also at negated Chunks and mark attributes extracted form those as negated
        Iterator<io.redlink.nlp.model.Token> words = section.getTokens();
        log.debug("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
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
