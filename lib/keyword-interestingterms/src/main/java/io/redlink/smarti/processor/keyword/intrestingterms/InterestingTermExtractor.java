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
import io.redlink.smarti.processing.SmartiAnnotations;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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
        log.debug("extract interestingTerms for {}[language: {}, fieldLang: {}, fields: {}]",getName(), language, similarityFields.getKey(), similarityFields.getValue());
        SolrQuery mltQuery = mltConfig.createMltQuery(language);
        for(String field : similarityFields.getValue()){
            if(StringUtils.isNotBlank(field)){
                mltQuery.add(MoreLikeThisParams.SIMILARITY_FIELDS,field);
            }
        }
        Analysis analysis = processingData.getAnnotation(SmartiAnnotations.ANALYSIS_ANNOTATION);
        try {
            beforeSimilarity(mltQuery, analysis);
        }catch (SimilarityNotSupportedException e) {
            log.warn("Similarity is not supported for this analysis");
            return;
        }
        //TODO: add callback that allows to add filters to the MLT query based on the analysis
        
        MltRequest mltRequest = new MltRequest(mltQuery, at.getSpan());
        log.trace("MLT Request: query:{} | text: {}", mltQuery, at.getSpan());
        NamedList<Object> response;
        try (SolrClient client = getClient()){
            response = client.request(mltRequest);
        } catch (SolrServerException | IOException e) {
            log.warn("Unable to search for interesting terms for {} with {} ({}: {})", processingData, getName(),
                    e.getClass().getSimpleName(), e.getMessage());
            log.debug("Stacktrace:", e);
            return;
        }
        
        NamedList<Object> interestingTermList = (NamedList<Object>)response.get("interestingTerms");
        if(interestingTermList.size() < 1) { //no interesting terms
            log.debug("No interesting Terms found");
            return;
        }
        Map<String,List<Token>> termMap = new HashMap<>();
        for(Iterator<Token> tokens = at.getTokens(); tokens.hasNext(); ){
            Token token = tokens.next();
            if(NlpUtils.isNoun(token) || NlpUtils.isAdjective(token)){
                //register for the span, stem and lemma
                new HashSet<>(Arrays.asList(token.getSpan(), NlpUtils.getStem(token), NlpUtils.getLemma(token)))
                    .forEach(key -> addTerm(termMap,key,locale, token));
            } //else ignore words with other POS tags
            
        }
        List<Entry<String,Float>> interestingTerms = new LinkedList<>();
        float maxBoost = 0; //search for the highest boost for normalization [0..1]
        for(Iterator<Entry<String,Object>> terms = interestingTermList.iterator(); terms.hasNext();){
            Entry<String,Object> e = terms.next();
            String term = e.getKey();
            float boost = ((Number)e.getValue()).floatValue();
            if(boost > maxBoost){
                maxBoost = boost;
            }
            interestingTerms.add(new ImmutablePair<String,Float>(term, boost));
        }
        log.debug("Solr MLT interesting Terms: {}", interestingTerms);
        for(Entry<String,Float> term : interestingTerms){
            String termKey = term.getKey();
            int fieldSepIdx = termKey.indexOf(':');
            String termName = fieldSepIdx > 0 ? termKey.substring(fieldSepIdx+1, termKey.length()) : termKey;
            List<Token> termTokens = termMap.get(termName);
            if(termTokens != null){
                for(Token token : termTokens){
                    Value<InterestingTerm> value = Value.value(new InterestingTerm(getKey(), termName), term.getValue()/maxBoost);
                    token.addValue(INTERESTING_TERM, value);
                    log.trace("mark {} as interesting Term {}", token, value);
                }
            }
        }
        

    }

    /**
     * Callback that allows implementations to modify the similarity query.
     * The base implementation is empty
     * based on the analysis (e.g. add filters based on the {@link Analysis#getClient() client}
     * @param mltQuery the Solr MLT query used for the similarity
     * @param analysis the analysis
     */
    protected void beforeSimilarity(SolrQuery mltQuery, Analysis analysis) throws SimilarityNotSupportedException {
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
