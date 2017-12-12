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

package io.redlink.smarti.processor.keyword.intrestingterms;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.Processor;
import io.redlink.nlp.api.model.Value;
import io.redlink.nlp.model.*;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.phrase.PhraseCategory;
import io.redlink.nlp.model.phrase.PhraseTag;
import io.redlink.nlp.model.pos.LexicalCategory;
import io.redlink.nlp.model.pos.Pos;
import io.redlink.nlp.model.pos.PosSet;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.redlink.nlp.model.NlpAnnotations.PHRASE_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.*;
import static io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermsConst.INTERESTING_TERM;

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
    
    /**
     * We interpret {@link PosSet#NOUNS} as well as {@link Pos#Numeral} as nouns
     */
    private final PosSet NOUNS = PosSet.union(PosSet.NOUNS).add(Pos.Numeral);
    private final PosSet INTERESTING_POS = PosSet.union(NOUNS, PosSet.ADJECTIVES);
    private final PosSet CONNECTING_POS = PosSet.of(LexicalCategory.Adposition);
    
    private static final Set<PhraseCategory> SECTION_CATEGORIES = EnumSet.of(PhraseCategory.Sentence);
    
    
    private static final Set<PhraseCategory> NOUN_PHRASE_CATEGORIES = EnumSet.of(
            PhraseCategory.NounPhrase,PhraseCategory.ForeignPhrase);
    
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
                log.warn("Section without {} annotation ([{},{}] {})", MESSAGE_ANNOTATION.getKey(), 
                        section.getStart(), section.getEnd(), section.getSpan());
            }
        }
    }

    private List<Token> createPhraseTokens(Section section, int msgIdx, Message message) {
        //int offset = section.getStart();
        //We look also at negated Chunks and mark attributes extracted form those as negated
        Iterator<Span> spans = section.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence, SpanTypeEnum.Token, SpanTypeEnum.Chunk));
        log.debug("Message {} - {}: {}", msgIdx, message.getOrigin(), message.getContent());
        InterestingTermPhrase activePhrase = null;
        List<InterestingTermPhrase> itPhrases = new LinkedList<>();
        //absolute offsets to end of sections that do not allow for noun chunks to cross over
        Set<Integer> sectionEnds = new HashSet<>(); 
        sectionEnds.add(section.getEnd());
        while(spans.hasNext()){
            Span span = spans.next();
            //int start = span.getStart() - offset; //start relative to the mesgIdx (as used for Tokens)
            //int end = span.getEnd() - offset; //end relative to the mesgIdx (as used for Tokens)
            switch(span.getType()){
            case Sentence:
                //TODO: close existing phrases
                sectionEnds.add(span.getEnd());
                break;
            case Chunk:
                Chunk phrase = (Chunk)span;
                PhraseTag phraseAnno = phrase.getAnnotation(PHRASE_ANNOTATION);
                if(phraseAnno != null){
                    if(!Collections.disjoint(SECTION_CATEGORIES,phraseAnno.getCategories())){
                        sectionEnds.add(span.getEnd());
                    }
                    if(!Collections.disjoint(NOUN_PHRASE_CATEGORIES, phraseAnno.getCategories())){
                        log.trace(" - add sectionEnd {} for {} [phraseAnno: {}]", phrase.getEnd(), phrase.getSpan(), phraseAnno);
                        sectionEnds.add(span.getEnd());
                    }
                }
                //TODO: maybe use some NounPhrase sub-types to create explicit phrases
                //OLD code
//                if(phraseAnno != null && //this chunk has a phrase annotation and it is a noun phrase
//                        !Collections.disjoint(NOUN_PHRASE_CATEGORIES,phraseAnno.getCategories()) &&
//                        //but not a phrase we want to ignore
//                        Collections.disjoint(IGNORED_PHRASE_CATEGORIES, phraseAnno.getCategories())){
//                    if(contextPhrase != null){
//                        if(phrase.getStart() > contextPhrase.end){
//                            if(contextPhrase.isInterestingPhrase()){
//                                Token token = createToken(section, msgIdx, contextPhrase);
//                                tokens.add(token);
//                                log.debug("  add InterstingNounPhrase token {}", token);
//                            } else {
//                                log.debug("  ignore none interesting noun phrase {}", contextPhrase);
//                            }
//                            contextPhrase = null;
//                        } else {
//                            contextPhrase.append(phrase, phraseAnno);
//                        }
//                    }
//                    if(contextPhrase == null){
//                        contextPhrase = new PhraseTokenData(phrase, phraseAnno);
//                    }
//                }
                break;
            case Token:
                Word word = new Word((io.redlink.nlp.model.Token)span);
                boolean completePhrase = false;
                if(activePhrase != null){
                    if(word.isInteresting() || word.isConnecting()){
                        log.trace(" - add {}  to phrase", word);
                        activePhrase.addWord(word);
                    } else {
                        log.trace(" - close phrase because of word {}",word);
                        completePhrase = true;
                    }
                } else if(word.isInteresting()){
                    log.debug(" - start phrase for {}", word);
                    activePhrase = new InterestingTermPhrase(word);
                }
                if(sectionEnds.remove(word.getEnd())){
                    log.trace(" - close phrase because of Section end");
                    completePhrase = true;
                }
                if(completePhrase && activePhrase != null){
                    activePhrase.complete();
                    log.debug(" - completed {} phrase {}", activePhrase.isInteresting() ? "interesting" : "", activePhrase);
                    if(activePhrase.isInteresting()){
                        itPhrases.add(activePhrase);
                    }
                    activePhrase = null;
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
        assert activePhrase == null; //after iterating the section spans it is expected that no active phrase is present
        return itPhrases.stream().map(phrase -> createToken(section, msgIdx, phrase)).collect(Collectors.toList());
    }

    /**
     * Creates a Keyword token for a noun phrase containing an interesting term
     * @param section
     * @param msgIdx
     * @param contextPhrase
     * @return
     */
    private Token createToken(Section section, int msgIdx, InterestingTermPhrase phrase) {
        Token token = new Token();
        token.setMessageIdx(msgIdx);
        token.setStart(phrase.getStart() - section.getStart());
        token.setEnd(phrase.getEnd() - section.getStart());
        token.setValue(section.getSpan().substring(token.getStart(), token.getEnd()));
        token.setType(Type.Keyword);
        token.addHint("interestingTerm");
        token.setConfidence(phrase.getConfidence().floatValue());
        //TODO: we could add sources and ranking if the token model would allow for
        return token;
    }
    
    final double sumProbability(double prop1, double prob2){
        return (prop1 + prob2)/(1 + (prop1*prob2));
    }
    
    class Word {
        
        private final io.redlink.nlp.model.Token token;
        private final boolean noun;
        private final boolean word;
        private final boolean interesting;
        private final boolean connecting;
        private final List<Value<InterestingTerm>> interestingTermAnnos;

        Word(io.redlink.nlp.model.Token token){
            this.token = token;
            this.word = NlpUtils.hasAlpha(token);
            this.connecting = word && NlpUtils.isOfPos(token, CONNECTING_POS);
            this.interesting = NlpUtils.isOfPos(token, INTERESTING_POS);
            this.noun = interesting && NlpUtils.isOfPos(token, NOUNS);
            this.interestingTermAnnos = token.getValues(INTERESTING_TERM);
        }
        
        public io.redlink.nlp.model.Token getToken() {
            return token;
        }
        
        public int getStart(){
            return token.getStart();
        }
        
        public int getEnd(){
            return token.getEnd();
        }
        
        public boolean isNoun() {
            return noun;
        }

        public boolean isWord() {
            return word;
        }

        public boolean isInteresting() {
            return interesting;
        }

        public boolean isConnecting() {
            return connecting;
        }
        
        public boolean hasInterestingTermAnno(){
            return !interestingTermAnnos.isEmpty();
        }

        public List<Value<InterestingTerm>> getInterestingTermAnnos() {
            return interestingTermAnnos;
        }
        
        @Override
        public String toString() {
            return "Word[" + token.getStart() + "," + token.getEnd() + " - " + token.getSpan() + "| pos: " + token.getValues(NlpAnnotations.POS_ANNOTATION) + "]";
        }
        
    }

    class InterestingTermPhrase {
        private final int start;
        private int numWords = 0;
        private int numNouns = 0;
        private int lastNounIdx = 0;
        private final List<Word> words = new LinkedList<>();
        private boolean completed = false;
        private Double ranking;
        private Double confidence;
        
        public InterestingTermPhrase(Word word) {
            this.start = word.getStart();
            addWord(word);
        }

        protected void addWord(Word word){
            if(completed){
                throw new IllegalStateException("Unable to add a word to a completed phrase");
            }
            if(word.isInteresting()){
                numWords++;
            }
            if(word.isNoun()){
                numNouns++;
                lastNounIdx = words.size();
            }
            this.words.add(word);
        }
        
        public int getStart() {
            return start;
        }
        
        public int getEnd(){
            return words.get(lastNounIdx).getEnd();
        }
        
        public String getSpan(){
            return words.get(0).getToken().getContext().getSpan().substring(getStart(),getEnd());
        }
        
        public void complete(){
            log.debug("> complete {}", this);
            completed = true;
            Map<String,List<Double>> sourceConfidences = new HashMap<>();
            words.subList(0, lastNounIdx + 1).stream()
                    .filter(Word::isInteresting)
                    .map(Word::getInterestingTermAnnos).flatMap(List::stream)
                    .forEach(ita -> {
                        String source = ita.value().getSource();
                        List<Double> sourceConf = sourceConfidences.get(source);
                        if(sourceConf == null){
                            sourceConf = new ArrayList<>(4);
                            sourceConfidences.put(source, sourceConf);
                        }
                        sourceConf.add(ita.probability());
                    });
            log.debug(" - Confidences by Source: {}", sourceConfidences);
            Map<String,Double> avrgSourceConfidences = sourceConfidences.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, en -> {
                            List<Double> confs = en.getValue();
                            return confs.stream().reduce(0d, (a,b) -> a + b) / confs.size();
                        }));
            log.debug(" - avrg confidence by source: {}", avrgSourceConfidences);
            confidence = avrgSourceConfidences.values().stream().reduce(0d, (a,b) -> sumProbability(a, b));
            log.debug(" - confidence: {}", confidence);
            ranking = sourceConfidences.values().stream().flatMap(List::stream)
                    .reduce(0d, (a,b) -> b > 0d ? a+1 : a)/numWords;
            log.debug(" - ranking: {}", ranking);
        }
        
        public boolean isInteresting() {
            if(!completed){
                throw new IllegalStateException("The interesting state is only available for completed ItPhrases");
            }
            return numNouns > 0 && ranking > 0d && confidence > 0d;
        }


        public Double getConfidence() {
            if(!completed){
                throw new IllegalStateException("Confidence is only available for completed ItPhrases");
            }
            return confidence;
        }
        
        public Double getRanking() {
            if(!completed){
                throw new IllegalStateException("Ranking is only available for completed ItPhrases");
            }
            return ranking;
        }
        
        @Override
        public String toString() {
            return "ItPhrase[" + getStart() + ", " + getEnd() + " - " + getSpan() + "| words: " + numWords + ",nouns: " + numNouns + "]";
        }
    }
    
    
}
