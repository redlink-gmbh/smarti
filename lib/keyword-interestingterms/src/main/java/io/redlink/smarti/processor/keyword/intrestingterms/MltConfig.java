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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.MoreLikeThisParams;

import java.util.*;
import java.util.stream.Collectors;

public class MltConfig {

    private int minDocFreq;
    private int minTermFreq;
    private int minWordLength;
    private boolean boost;
    private boolean interstingTerms;
    private String filterQuery;
    private int maxTerms;

    private Map<String, Collection<String>> langSimilarityFields = new HashMap<>();
    
    /**
     * Returns an instance of schema configuration with <ul>
     * <li> {@link #getIdField()}: id
     * <li> {@link #getTitleField()}: title
     * <li> {@link #getAltTitleField()}: alt_title
     * <li> {@link #getTypeField()}: type
     * <li> {@link #getUriField()}: uri
     * </ul>
     * @return
     */
    public static MltConfig getDefault(){
        MltConfig conf = new MltConfig();
        conf.interstingTerms = true;
        conf.boost = true;
        conf.minDocFreq = 1;
        conf.minTermFreq = 1;
        conf.minWordLength = 3;
        conf.filterQuery = null; //no filter query
        conf.maxTerms = 20;
        return conf;
    }

    public Map.Entry<String,Collection<String>> getSimilarityFields(String lang) {
        return getSimilarityFields(lang, true);
    }
    public Map.Entry<String,Collection<String>> getSimilarityFields(String lang, boolean fallback) {
        lang = StringUtils.lowerCase(lang, Locale.ROOT);
        Collection<String> langFields = langSimilarityFields.get(lang);
        if(fallback){
            int sepIdx = StringUtils.indexOfAny(lang, '-','_');
            if(sepIdx > 0){
                lang = lang.substring(0, sepIdx);
                langFields = langSimilarityFields.get(lang);
            }
            if(langFields == null && lang != null){
                lang = null;
                langFields = langSimilarityFields.get(lang);
            }
        } //else no fallback
        return langFields == null ? null : new ImmutablePair<>(lang, langFields);
    }
    
    public MltConfig addSimilarityFields(String lang, String field){
        if(StringUtils.isEmpty(field)){
            return this;
        }
        lang = StringUtils.lowerCase(lang, Locale.ROOT);
        Collection<String> langFields = langSimilarityFields.get(lang);
        if(langFields == null){
            langFields = new LinkedHashSet<>();
            langSimilarityFields.put(lang, langFields);
        }
        langFields.add(field);
        return this;
    }

    public MltConfig setSimilarityFields(String lang, Collection<String> similarityFields) {
        lang = StringUtils.lowerCase(lang, Locale.ROOT);
        if(CollectionUtils.isEmpty(similarityFields)){
            langSimilarityFields.remove(lang);
        } else {
            Collection<String> langFields = similarityFields.stream()
                    .filter(StringUtils::isNoneBlank)
                    .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
            if(CollectionUtils.isNotEmpty(langFields)){
                langSimilarityFields.put(lang, langFields);
            } else {
                langSimilarityFields.remove(lang);
            }
        }
        return this;
    }

    public int getMinDocFreq() {
        return minDocFreq;
    }

    public MltConfig setMinDocFreq(int minDocFreq) {
        this.minDocFreq = minDocFreq;
        return this;
    }

    public int getMinTermFreq() {
        return minTermFreq;
    }

    public MltConfig setMinTermFreq(int minTermFreq) {
        this.minTermFreq = minTermFreq;
        return this;
    }

    public int getMinWordLength() {
        return minWordLength;
    }

    public MltConfig setMinWordLength(int minWordLength) {
        this.minWordLength = minWordLength;
        return this;
    }

    public boolean isBoost() {
        return boost;
    }

    public MltConfig setBoost(boolean boost) {
        this.boost = boost;
        return this;
    }
    
    public boolean isInterstingTerms() {
        return interstingTerms;
    }
    
    public MltConfig setInterstingTerms(boolean interstingTerms) {
        this.interstingTerms = interstingTerms;
        return this;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public MltConfig setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
        return this;
    }

    public SolrQuery createMltQuery(String lang) {
        return initMlt(new SolrQuery(), lang);
    }
        
    public SolrQuery initMlt(SolrQuery query, String lang){
        query.set(MoreLikeThisParams.MLT,true);
        query.set(MoreLikeThisParams.BOOST, boost);
        query.set(MoreLikeThisParams.INTERESTING_TERMS, interstingTerms ? 
                MoreLikeThisParams.TermStyle.DETAILS.name() : 
                    MoreLikeThisParams.TermStyle.NONE.name());
        query.set(MoreLikeThisParams.MIN_DOC_FREQ,minDocFreq);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, minTermFreq);
        query.set(MoreLikeThisParams.MIN_WORD_LEN, minWordLength);
        query.set(MoreLikeThisParams.MAX_QUERY_TERMS, maxTerms);
        if(filterQuery != null){
            query.addFilterQuery(filterQuery);
        }
        return query;
    }

    public int getMaxTerms() {
        return maxTerms;
    }

    public void setMaxTerms(int maxTerms) {
        this.maxTerms = maxTerms;
    }

    @Override
    public String toString() {
        return "MltConfig [boost=" + boost + ", similarityFields=" + langSimilarityFields + ", minDocFreq=" + minDocFreq
                + ", minTermFreq=" + minTermFreq + ", minWordLength=" + minWordLength + ", maxTerms=" + maxTerms
                + ", interstingTerms=" + interstingTerms + ", filterQuery=" + filterQuery + "]";
    }
    
    
}
