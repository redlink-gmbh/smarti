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

package io.redlink.smarti.lib.solr.iterms;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.ClosureUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrInterestingTermsUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SolrInterestingTermsUtils.class);
    
    public static Stream<ContextWord> extractInterestingTerms(SolrClient solrClient, SolrQuery solrQuery, String context) throws SolrServerException, IOException {
        //NOTE: by default we use the query time analyzer to avoid char offset changes by the FlattenGraphFactory
        return extractInterestingTerms(solrClient, solrQuery, context, true);
    }
    public static Stream<ContextWord> extractInterestingTerms(SolrClient solrClient, SolrQuery solrQuery, String context, boolean queryAnalyzer) throws SolrServerException, IOException {
        if(ArrayUtils.isEmpty(solrQuery.getMoreLikeThisFields())){
            throw new IllegalArgumentException("The parsed SolrQuery MUST HAVE the param '" 
                    + MoreLikeThisParams.SIMILARITY_FIELDS + "' set!");
        }
        //we make a MLT query to get the interesting terms
        //we do not want any results
        solrQuery.setRows(0);
        //we search for interesting terms in the MLT Context field
        solrQuery.setMoreLikeThisBoost(true);
        solrQuery.set(MoreLikeThisParams.INTERESTING_TERMS,"details");
        
        log.trace("InterestingTerms QueryParams: {}", solrQuery);
        
        NamedList<Object> response = solrClient.request(new MltRequest(solrQuery, context));
        @SuppressWarnings("unchecked")
        NamedList<Object> interestingTermList = (NamedList<Object>)response.get("interestingTerms");
        if(interestingTermList != null && interestingTermList.size() > 0) { //interesting terms present
            //Do make it easier to combine context params with other ensure that the maximum boost is 1.0
            AtomicReference<Float> norm = new AtomicReference<>(-1f);
            
            List<ContextTerm> ctxTerms = StreamSupport.stream(interestingTermList.spliterator(), false)
                .sorted((a,b) -> Double.compare(((Number)b.getValue()).doubleValue(), ((Number)a.getValue()).doubleValue()))
                .map(e -> {
                    final float relevance;
                    if(norm.get() < 0) {
                        norm.set(1f/((Number)e.getValue()).floatValue());
                        relevance = 1f;
                    } else {
                        relevance = (((Number)e.getValue()).floatValue() * norm.get());
                    }
                    return new ContextTerm(e.getKey(), relevance);
                })
                .collect(Collectors.toList());
            //build an Index of {field,term} -> [<AnalysisInfo>]
            final WordAnalysisIdx waIdx = new WordAnalysisIdx();
            analyseTerm(solrClient, ctxTerms.stream().map(ContextTerm::getField).collect(Collectors.toSet()), context, queryAnalyzer)
                .forEach((field,terms) -> waIdx.add(field,terms));
            //now we can get the actual words for the Terms returned by the Solr MLT interesting Terms element
            log.debug("write contextQueryTerms");
            return ctxTerms.stream()
                    .map(ctxTerm -> new ContextWord(ctxTerm, waIdx.getWordAnalysis(ctxTerm.getField(), ctxTerm.getTerm())))
                    .filter(cw -> CollectionUtils.isNotEmpty(cw.getWordAnalysis()));
        }
        return Stream.empty(); //no Interesting Terms found or SolrException
    }
    /**
     * This method allows to analyse the parsed text with the Solr Analyser of the parsed field name.
     * This is useful when one needs to match Solr terms (from the inverted index) with sections of
     * the text or Terms and Keywords extracted from that text
     * @param client the SolrClient used to execute the requests
     * @param field the Solr field
     * @param text the text to analyse
     * @throws SolrException
     * @throws IOException
     * @throws SolrServerException
     */
    public static Map<String,List<WordAnalysis>> analyseTerm(SolrClient client, Set<String> fields, String text) throws SolrException, IOException, SolrServerException {
        return analyseTerm(client, fields, text, false);
    }
    /**
     * This method allows to analyse the parsed text with the Solr Analyser of the parsed field name.
     * This is useful when one needs to match Solr terms (from the inverted index) with sections of
     * the text or Terms and Keywords extracted from that text
     * @param client the SolrClient used to execute the requests
     * @param field the Solr field
     * @param text the text to analyse
     * @param queryAnalyzer if <code>true</code> the query time analyzer will be used. Otherwise the index time analyzer is used
     * @throws SolrException
     * @throws IOException
     * @throws SolrServerException
     */
    public static Map<String,List<WordAnalysis>> analyseTerm(SolrClient client, Set<String> fields, String text, boolean queryAnalyzer) throws SolrException, IOException, SolrServerException {
        FieldAnalysisRequest request = new FixedFieldAnalysisRequest();
        request.setFieldNames(new ArrayList<>(fields));
        if(queryAnalyzer) {
            request.setQuery(text);
        } //else {
        request.setFieldValue(text); //field value is required :(
        //}
        
        FieldAnalysisResponse respone = request.process(client);
        Map<String,List<WordAnalysis>> fieldAnalysis = new HashMap<>();
        respone.getAllFieldNameAnalysis().forEach(e -> {
            org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis analysis = e.getValue();
            final AnalysisPhase result; //the analysis result is the output of the last AnalysisPhase
            Iterable<AnalysisPhase> phrases = queryAnalyzer ? analysis.getQueryPhases() : analysis.getIndexPhases();
            if(phrases instanceof List){ //in current SolrJ this is a list 
                result = ((List<AnalysisPhase>)phrases).get(((List) phrases).size() - 1);
            } else { //but provide a fallback if not ...
                result = IteratorUtils.forEachButLast(phrases.iterator(), ClosureUtils.nopClosure());
            }
            fieldAnalysis.put(e.getKey(), result.getTokens().stream()
                    .map(t -> createWordAnalysis(t,text))
                    .collect(Collectors.toList()));
        });
        return fieldAnalysis;
    }
    /**
     * Gets the tokens of the parsed text that are indexed by Solr based in the field configurations. The returned map
     * uses the field as key and returns the list of tokens as value. <p>
     * This will remove parts of the text that are not indexed in Solr (e.g. stop words)
     * @param client the {@link SolrClient} used to send the {@link FieldAnalysisRequest} to
     * @param fields the fields used to analyze the parsed text
     * @param text the text to analyze
     * @param queryAnalyzer <code>true</code> to use the query time analyzer <code>false</code> for the index time analyzer
     * @return the map with the field as key and the list of indexed tokens as value.
     * @throws SolrException
     * @throws IOException
     * @throws SolrServerException
     */
    public static Map<String,List<String>> getIndexedTokens(SolrClient client, Set<String> fields, String text, boolean queryAnalyzer) throws SolrException, IOException, SolrServerException {
    
        FieldAnalysisRequest request = new FixedFieldAnalysisRequest();
        request.setFieldNames(new ArrayList<>(fields));
        if(queryAnalyzer) {
            request.setQuery(text);
        } //else {
        request.setFieldValue(text); //field value is required :(
        //}
        Map<String,List<String>> fieldTokens = new HashMap<>();
        FieldAnalysisResponse respone = request.process(client);
        respone.getAllFieldNameAnalysis().forEach(e -> {
            org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis analysis = e.getValue();
            fieldTokens.put(e.getKey(), parseTokens(text, analysis, queryAnalyzer));
        });
        return fieldTokens;
    }
    /**
     * Gets the tokens of the parsed text that are indexed by Solr based on the configuration of the parsed field. <p>
     * This will remove parts of the text that are not indexed in Solr (e.g. stop words)
     * @param client the {@link SolrClient} used to send the {@link FieldAnalysisRequest} to
     * @param field the name of the field used to analyze the parsed text
     * @param text the text to analyze
     * @param queryAnalyzer <code>true</code> to use the query time analyzer <code>false</code> for the index time analyzer
     * @return the indexed tokens as present in the parsed text (NOT the terms as stored in the inverted index)
     * @throws SolrException
     * @throws IOException
     * @throws SolrServerException
     */
    public static List<String> getIndexedTokens(SolrClient client, String field, String text, boolean queryAnalyzer) throws SolrException, IOException, SolrServerException {
        
        FieldAnalysisRequest request = new FixedFieldAnalysisRequest();
        request.setFieldNames(Collections.singletonList(field));
        if(queryAnalyzer) {
            request.setQuery(text);
        } //else {
        request.setFieldValue(text); //field value is required :(
        //}
        FieldAnalysisResponse respone = request.process(client);
        return parseTokens(text, respone.getFieldNameAnalysis(field), queryAnalyzer);
    }
    
    private static List<String> parseTokens(String text,
            org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis analysis, boolean queryAnalyzer) {
        List<String> tokens = new LinkedList<>();
        final AnalysisPhase result; //the analysis result is the output of the last AnalysisPhase
        Iterable<AnalysisPhase> phrases = queryAnalyzer ? analysis.getQueryPhases() : analysis.getIndexPhases();
        if(phrases instanceof List){ //in current SolrJ this is a list 
            result = ((List<AnalysisPhase>)phrases).get(((List) phrases).size() - 1);
        } else { //but provide a fallback if not ...
            result = IteratorUtils.forEachButLast(phrases.iterator(), ClosureUtils.nopClosure());
        }
        int end = 0;
        int start = 0;
        int pos = -1;
        
        for(TokenInfo ti : result.getTokens()){
            if(ti.getStart() >= end && ti.getEnd() > end) {
                //new Token
                tokens.add(text.substring(ti.getStart(), ti.getEnd()));
                start = ti.getStart();
                end = ti.getEnd();
                pos = ti.getPosition();
            } else if(ti.getEnd() > end && ti.getPosition() == pos) {
                //longer token with the same position as the current
                tokens.set(tokens.size()-1, text.substring(start, ti.getEnd()));
                end = ti.getEnd();
            }
        }
        return tokens;
    }
    /**
     * Helper class holding a lookup table for analyzed words (field:term) to words in the analyzed text
     * @author Rupert Westenthaler
     *
     */
    static class WordAnalysisIdx {
        
        private final Map<Pair<String,String>, Collection<WordAnalysis>> tokenIndex = new HashMap<>();
        
        public void add(String field, Iterable<WordAnalysis> words){
            words.forEach(wa -> tokenIndex.computeIfAbsent(
                    new ImmutablePair<String, String>(field, wa.getTerm()), k -> new LinkedList<>()).add(wa));
        }
        
        public Collection<WordAnalysis> getWordAnalysis(String field, String term){
            return tokenIndex.get(new ImmutablePair<String,String>(field, term));
        }
        
        public Set<String> getWords(String field, String term){
            return tokenIndex.getOrDefault(new ImmutablePair<String,String>(field, term), Collections.emptySet()).stream()
                .map(WordAnalysis::getWord)
                .collect(Collectors.toSet());
        }
    }
    
    /*
     * Workaround for a Bug in SolrJ FixedFieldAnalysisResponse
     * 
     * The FixedFieldAnalysisResponse#buildPhases(..) can not handle situations where
     * a CharFilter is part of the AnalysisPipeline.
     * In those cases the phaseNL does simple contain the parsed Text and not a
     * further NamedList. So the buildPhrase runs into an ClassCastException.
     * 
     * This implementation performs an instanceof check and simple ignores CharFilter
     * information present in the Response
     * 
     * To make this work it is required to call the default visible constructor of the
     * AnalysisPhase via reflection :(
     * 
     */
    private final static class FixedFieldAnalysisRequest extends FieldAnalysisRequest {

        private static final long serialVersionUID = -5119011666718786733L;

        @Override
        protected FieldAnalysisResponse createResponse(SolrClient client) {
            if (getFieldTypes() == null && getFieldNames() == null) {
                throw new IllegalStateException("At least one field type or field name need to be specified");
            }
            if (getFieldValue() == null) {
                throw new IllegalStateException("The field value must be set");
            }
            return new FixedFieldAnalysisResponse();
        }
    }
    /*
     * Works around a bug where {@link FieldAnalysisResponse} fails if the analysis
     * includes a {@link CharFilter}
     * 
     * See comment on FixedFieldAnalysisRequest for more information
     *
     */
    private final static class FixedFieldAnalysisResponse extends FieldAnalysisResponse {

        private static final long serialVersionUID = -7771137722322121975L;

        @Override
        protected List<AnalysisPhase> buildPhases(NamedList<Object> phaseNL) {
            List<AnalysisPhase> phases = new ArrayList<>(phaseNL.size());
            for (Map.Entry<String, Object> phaseEntry : phaseNL) {
                if(phaseEntry.getValue() instanceof List){
                    final AnalysisPhase phase;
                    try {
                        Constructor<AnalysisPhase> constructor = AnalysisPhase.class.getDeclaredConstructor(String.class);
                        constructor.setAccessible(true);
                        phase = constructor.newInstance(phaseEntry.getKey());
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | 
                            IllegalArgumentException | InvocationTargetException e) {
                        throw new SolrException(ErrorCode.UNKNOWN, "Unable to instanciate AnalysisPhrase using reflection ("
                                + e.getClass().getSimpleName() + " - " + e.getMessage() + ")!", e);
                    }
                    @SuppressWarnings("unchecked")
                    List<NamedList<Object>> tokens = (List<NamedList<Object>>)phaseEntry.getValue();
                    for (NamedList<Object> token : tokens) {
                        TokenInfo tokenInfo = buildTokenInfo(token);
                        phase.getTokens().add(tokenInfo);
                    }
                    phases.add(phase);
                } // CharFilter do not define the information needed to construct AnalysisPhrases ... as we do not need those anyway we just ignore them
            }
            return phases;
        }
    }
    
    private static WordAnalysis createWordAnalysis(TokenInfo token, String text) {
        return new WordAnalysis(text.substring(token.getStart(), token.getEnd()), token.getText(), token.getStart(), token.getEnd(), token.getPosition());
    }

}
