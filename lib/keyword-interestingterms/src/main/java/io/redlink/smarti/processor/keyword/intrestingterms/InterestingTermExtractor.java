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
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.NlpAnnotations;
import io.redlink.nlp.model.Token;
import io.redlink.nlp.model.util.NlpUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.processing.SmartiAnnotations;
import io.redlink.smarti.lib.solr.iterms.SolrInterestingTermsUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermsConst.INTERESTING_TERM;

/**
 * To be sub-classed 
 * @author westei
 *
 */
public abstract class InterestingTermExtractor extends Processor {

    protected InterestingTermExtractor(String name) {
        super("keyword.interestingterms." + name, "Interesting Term Extractor for Solr Core " + name, Phase.extraction);
    }

    @Override
    public Map<String, Object> getDefaultConfiguration() {
        return Collections.emptyMap();
    }
    
    /**
     * The SolrClient used by this linker
     * @return
     */
    protected abstract SolrClient getClient() throws SolrServerException;

    protected abstract MltConfig getMltConf();

    /**
     * Getter for the {@link ContentSectionProvider} used for linking parsed {@link AnalyzedText}s. Override this to
     * use a different {@link ContentSectionProvider} implementation
     * @return {@link ContentSectionProvider#SECTIONS}
     */
    protected ContentSectionProvider getContentProvider(){
        return ContentSectionProvider.SECTIONS;
    }

    @Override
    protected final void init() {
        try (SolrClient client = getClient()){
            //lets ping the solrServer on initialization
            log.debug("ping {}", client);
            SolrPingResponse ping = client.ping();
            log.debug("ping respone: {}", ping);
        } catch (SolrServerException | IOException e) {
            log.warn("Unable to ping SolrClient for {} during initialization ({} - {})", getName(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    protected final void doProcessing(ProcessingData processingData) {
        final AnalyzedText at = NlpUtils.getOrInitAnalyzedText(processingData);
        if(at == null){ //no plain text  ... nothing to do
            return;
        }
        //TODO: Think about language support
        String language = processingData.getLanguage();
        Locale locale = language == null ? Locale.ROOT : Locale.forLanguageTag(language);
        MltConfig mltConfig = getMltConf();
        if(!mltConfig.isInterstingTerms()){
            log.warn("enabling 'interestingTerms' on MLT configuration of {}. This is expected to be 'true' so please"
                    + "check the MltConfig parsed by this implementation!");
            mltConfig.setInterstingTerms(true); //we need interesting terms
        }
        Entry<String, Collection<String>> similarityFields = mltConfig.getSimilarityFields(language);
        if(similarityFields == null){
            log.debug("language '{}' not supported by {}", language, getName());
            return; //nothing to do
        }
        SolrQuery mltQuery = mltConfig.createMltQuery(language);
        similarityFields.getValue().stream()
            .filter(StringUtils::isNoneBlank)
            .forEach(field -> mltQuery.add(MoreLikeThisParams.SIMILARITY_FIELDS,field));
        log.debug("extract interestingTerms for {}[language: {}, fieldLang: {}, mltQuery: {}]",getName(), language, similarityFields.getKey(), mltQuery);

        Analysis analysis = processingData.getAnnotation(SmartiAnnotations.ANALYSIS_ANNOTATION);
        Conversation conversation = processingData.getAnnotation(SmartiAnnotations.CONVERSATION_ANNOTATION);
        try {
            beforeSimilarity(mltQuery, analysis, conversation);
        }catch (SimilarityNotSupportedException e) {
            log.warn("Similarity is not supported for this analysis");
            return;
        }
        //TODO: add callback that allows to add filters to the MLT query based on the analysis
        

        //we need to lookup words we consider candidates (AJD and N)
        Map<String,List<Token>> wordMap = new HashMap<>();
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(at.getTokens(), Spliterator.ORDERED),false)
            .filter(t -> NlpUtils.isNoun(t) || NlpUtils.isAdjective(t))
            .forEach(t -> addTerm(wordMap, t.getSpan(), locale, t));

        log.trace("MLT Request: query:{} | text: {}", mltQuery, at.getSpan());
        try (SolrClient client = getClient()){
            SolrInterestingTermsUtils.extractInterestingTerms(client, mltQuery, at.getSpan())
            .flatMap(wc -> wc.getWords().stream()
                    .filter(wordMap::containsKey)
                    .map(w -> new ImmutablePair<>(w, wc.getRelevance())))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (v1,v2) -> Math.max(v1, v2)))
            .forEach((word, relevance) -> {
                wordMap.getOrDefault(word,Collections.emptyList()).stream()
                    .forEach(token -> {
                        Value<InterestingTerm> value = Value.value(new InterestingTerm(getKey(), word), relevance);
                        log.trace("mark {} as {}", token, value);
                        token.addValue(INTERESTING_TERM, value);
                    });
            });
        } catch (SolrServerException | IOException e) {
            log.warn("Unable to search for interesting terms for {} with {} ({}: {})", processingData, getName(),
                    e.getClass().getSimpleName(), e.getMessage());
            log.debug("Stacktrace:", e);
            return;
        }
    }

    /**
     * Callback that allows implementations to modify the similarity query.
     * The base implementation is empty
     * based on the analysis (e.g. add filters based on the {@link Analysis#getClient() client}
     * @param mltQuery the Solr MLT query used for the similarity
     * @param analysis the analysis
     * @param conversation the analysed conversation
     */
    protected void beforeSimilarity(SolrQuery mltQuery, Analysis analysis, Conversation conversation) throws SimilarityNotSupportedException {
    }

    private void addTerm(Map<String, List<Token>> termMap, String stem, Locale locale, Token token) {
        if(stem == null){
            return;
        }
        List<Token> termTokens = termMap.get(stem);
        if(termTokens == null){
            termTokens = new LinkedList<>();
            termMap.put(stem, termTokens);
        }
        termTokens.add(token);
        //also add a lower case variant
        String lcStem = stem.toLowerCase(locale);
        if(!lcStem.equals(stem)){
            termTokens = termMap.get(lcStem);
            if(termTokens == null){
                termTokens = new LinkedList<>();
                termMap.put(lcStem, termTokens);
            }
            termTokens.add(token);
        }
    }
    
    protected Set<String> getLanguages(final AnalyzedText at) {
        List<Value<String>> langAnnos = at.getValues(NlpAnnotations.LANGUAGE_ANNOTATION);
        Set<String> languages = new HashSet<>();
        for(Value<String> langAnno : langAnnos){
            if(langAnno.probability() == Value.UNKNOWN_PROBABILITY || langAnno.probability() >= 0.33f){
                String lang = langAnno.value();
                if(lang.indexOf('-') > 0){
                    String baseLang = lang.substring(0, lang.indexOf('-'));
                    languages.add(baseLang);
                }
                languages.add(lang);
            }
        }
        return languages;
    }
    
    public static class SimilarityNotSupportedException extends RuntimeException {
        
        private static final long serialVersionUID = -3140358965019251641L;

        public SimilarityNotSupportedException(){
            super();
        }
        public SimilarityNotSupportedException(String message){
            super(message);
        }
    }
    
}
