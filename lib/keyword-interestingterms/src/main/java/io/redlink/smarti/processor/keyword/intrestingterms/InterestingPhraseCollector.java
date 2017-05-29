/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.processor.keyword.intrestingterms;

import static io.redlink.nlp.model.NlpAnnotations.PHRASE_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.CONVERSATION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.MESSAGE_IDX_ANNOTATION;
import static io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermsConst.INTERESTING_TERM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.api.model.Value;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Chunk;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.Span;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.phrase.PhraseCategory;
import io.redlink.nlp.model.phrase.PhraseTag;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.api.QueryPreparator;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

/**
 * This class processes Nouns and NounPhrases that are also marked with a
 * {@link InterestingTermsConst#INTERESTING_TERM} annotation. It add
 * {@link Token} of type <code>KEYWORD</code> for those
 * <p>
 * This {@link QueryPreparator} only works if also an {@link InterestingTermExtractor} is
 * present in the same extraction pipeline. The {@link InterestingTermExtractor} needs to
 * run first. 
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public final class InterestingPhraseCollector extends Processor {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Set<PhraseCategory> NOUN_PHRASE_CATEGORIES = EnumSet.of(
            PhraseCategory.NounPhrase,PhraseCategory.NounHeadedPhrase,PhraseCategory.ForeignPhrase);
    
    public InterestingPhraseCollector(){
        super("keyword.interestingphrasecollector","Interesting Phrase Collector", Phase.extraction, 1); //needs to run after IterestingTermExtractor
    }
    
    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap(); //TODO
    }
    
    @Override
    protected void init() {
        //no op
    }
    
    @Override
    public void doProcessing(ProcessingData processingData) {
        Optional<AnalyzedText> oat = NlpUtils.getAnalyzedText(processingData);
        if(!oat.isPresent()) {
            return; //nothing todo
        }
        AnalyzedText at = oat.get();
        Conversation conv = processingData.getAnnotation(CONVERSATION_ANNOTATION);
        if(conv == null){
            log.warn("Unable to process ProcessingData without a '{}' annotation", CONVERSATION_ANNOTATION);
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
                        conv.getTokens().addAll(createPhraseTokens(section, msgIdx, message));
                    }
                }
            } else { //invalid section
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_ANNOTATION.getKey(), 
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
                Value<PhraseTag> phraseAnno = phrase.getValue(PHRASE_ANNOTATION);
                if(phraseAnno != null && //this chunk has a phrase annotation and it is a noun phrase
                        !Collections.disjoint(NOUN_PHRASE_CATEGORIES,phraseAnno.value().getCategories())){
                    if(contextPhrase != null){
                        if(phrase.getStart() > contextPhrase.end){
                            if(contextPhrase.isInterestingPhrase()){
                                Token token = createToken(section, msgIdx, contextPhrase);
                                tokens.add(token);
                                log.debug("  add InterstingNounPhrase token {}", token);
                            } else {
                                log.debug("  ignore none interesting noun phrase {}", contextPhrase);
                            }
                            contextPhrase = null;
                        } else {
                            contextPhrase.append(phrase, phraseAnno.value());
                        }
                    }
                    if(contextPhrase == null){
                        contextPhrase = new PhraseTokenData(phrase, phraseAnno.value());
                    }
                }
                break;
            case Token:
                io.redlink.nlp.model.Token word = (io.redlink.nlp.model.Token)span;
                boolean isNoun = NlpUtils.isNoun(word);
                boolean inPhrase = contextPhrase != null && contextPhrase.end >= word.getEnd();
                List<Value<String>> interestingTermAnnos = word.getValues(INTERESTING_TERM);
                if(!interestingTermAnnos.isEmpty()){
                    if(!inPhrase && isNoun){ //add a single word keyword
                        Token token = createToken(section, msgIdx, word, interestingTermAnnos);
                        tokens.add(token);
                        log.debug("create InterestingTerm token {}", token);
                    } else if(inPhrase){
                        contextPhrase.addInterestingTerms(interestingTermAnnos);
                    }
                }
                if(contextPhrase != null && NlpUtils.isNoun(word) && 
                        contextPhrase.end >= word.getEnd()){
                    contextPhrase.addNoun(word);
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
        if(contextPhrase != null && contextPhrase.isInterestingPhrase()){
            Token token = createToken(section, msgIdx, contextPhrase);
            tokens.add(token);
            log.debug("  add InterstingNounPhrase token {}", token);
        }
        return tokens;
    }
    /**
     * Creates a Keyword token for a single word annotated as interesting term
     * @param section
     * @param msgIdx
     * @param word
     * @param interestingTermAnnos
     * @return
     */
    private Token createToken(Section section, int msgIdx, io.redlink.nlp.model.Token word,
            List<Value<String>> interestingTermAnnos) {
        assert !interestingTermAnnos.isEmpty(); 
        Token token = new Token();
        token.setMessageIdx(msgIdx);
        token.setStart(word.getStart() - section.getStart());
        token.setEnd(word.getEnd() - section.getStart());
        token.setValue(section.getSpan().substring(token.getStart(), token.getEnd()));
        token.setType(Type.Keyword);
        token.addHint("interestingTerm");
        float conf = -1;
        for(Value<String> ita : interestingTermAnnos){
            if(conf < 0){
                conf = (float)ita.probability();
            } else {
                conf = sumProbability(conf, (float)ita.probability());
            }
        }
        if(conf >= 0){
            token.setConfidence(conf);
        }
        return token;
    }
    /**
     * Creates a Keyword token for a noun phrase containing an interesting term
     * @param section
     * @param msgIdx
     * @param contextPhrase
     * @return
     */
    private Token createToken(Section section, int msgIdx, PhraseTokenData contextPhrase) {
        Token token = new Token();
        token.setMessageIdx(msgIdx);
        token.setStart(contextPhrase.start - section.getStart());
        token.setEnd(contextPhrase.end - section.getStart());
        token.setValue(section.getSpan().substring(token.getStart(), token.getEnd()));
        token.setType(Type.Keyword);
        token.addHint("interestingTerm");
        float conf = contextPhrase.getConfidence();
        if(conf >= 0){
            token.setConfidence(contextPhrase.getConfidence());
        }
        return token;
    }
    
    final float sumProbability(float prop1, float prob2){
        return (prop1 + prob2)/(1 + (prop1*prob2));
    }


    class PhraseTokenData {
        
        final int start;
        int end;
        
        final List<PhraseTag> tags = new LinkedList<>();
        final List<io.redlink.nlp.model.Token> nouns = new LinkedList<>();
        private final List<Value<String>> interestingTerms = new LinkedList<>();
        
        
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
        
        public void addInterestingTerm(Value<String> interestingTermAnno){
            this.interestingTerms.add(interestingTermAnno);
        }
        
        public void addInterestingTerms(List<Value<String>> interestingTermAnnos) {
            this.interestingTerms.addAll(interestingTermAnnos);
        }
        
        public boolean isInterestingPhrase(){
            return !interestingTerms.isEmpty();
        }
        
        /**
         * The confidence based on the sum of all {@link #interestingTerms} contained in this phrase
         * @return the confidence <code>[0..1]</code>, <code>-1</code> if unknown
         */
        public float getConfidence(){
            float conf = -1f;
            for(Value<String> ita : interestingTerms){
                if(conf < 0){
                    conf = (float)ita.probability();
                } else {
                    conf = sumProbability(conf, (float)ita.probability());
                }
            }
            return conf;
        }

        @Override
        public String toString() {
            return "PhraseTokenData [start=" + start + ", end=" + end + ", nouns=" + nouns + ", interestingTerms="
                    + interestingTerms + "]";
        }

    }
    
}
