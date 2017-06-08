package io.redlink.smarti.processor.keyword.intrestingterms;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.MoreLikeThisParams;

public class MltConfig {

    private List<String> similarityFields;
    private int minDocFreq;
    private int minTermFreq;
    private int minWordLength;
    private boolean boost;
    private boolean interstingTerms;
    private String filterQuery;
    private int maxTerms;

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
        conf.similarityFields = new LinkedList<String>(); //empty default search field
        conf.filterQuery = null; //no filter query
        conf.maxTerms = 20;
        return conf;
    }

    public List<String> getSimilarityFields() {
        return similarityFields;
    }
    
    public void addSimilarityFields(String field){
        if(field != null){
            similarityFields.add(field);
        }
    }

    public void setSimilarityFields(List<String> similarityFields) {
        this.similarityFields = similarityFields == null ? new LinkedList<>() : similarityFields;
    }

    public int getMinDocFreq() {
        return minDocFreq;
    }

    public void setMinDocFreq(int minDocFreq) {
        this.minDocFreq = minDocFreq;
    }

    public int getMinTermFreq() {
        return minTermFreq;
    }

    public void setMinTermFreq(int minTermFreq) {
        this.minTermFreq = minTermFreq;
    }

    public int getMinWordLength() {
        return minWordLength;
    }

    public void setMinWordLength(int minWordLength) {
        this.minWordLength = minWordLength;
    }

    public boolean isBoost() {
        return boost;
    }

    public void setBoost(boolean boost) {
        this.boost = boost;
    }
    
    public boolean isInterstingTerms() {
        return interstingTerms;
    }
    
    public void setInterstingTerms(boolean interstingTerms) {
        this.interstingTerms = interstingTerms;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public SolrQuery createMltQuery() {
        return initMlt(new SolrQuery());
    }
        
    public SolrQuery initMlt(SolrQuery query){
        query.set(MoreLikeThisParams.MLT,true);
        query.set(MoreLikeThisParams.BOOST, boost);
        query.set(MoreLikeThisParams.INTERESTING_TERMS, interstingTerms ? 
                MoreLikeThisParams.TermStyle.DETAILS.name() : 
                    MoreLikeThisParams.TermStyle.NONE.name());
        query.set(MoreLikeThisParams.MIN_DOC_FREQ,minDocFreq);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, minTermFreq);
        query.set(MoreLikeThisParams.MIN_WORD_LEN, minWordLength);
        query.set(MoreLikeThisParams.MAX_QUERY_TERMS, maxTerms);
        for(String field : similarityFields){
            if(StringUtils.isNotBlank(field)){
                query.add(MoreLikeThisParams.SIMILARITY_FIELDS,field);
            }
        }
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
        return "MltConfig [boost=" + boost + ", similarityFields=" + similarityFields + ", minDocFreq=" + minDocFreq
                + ", minTermFreq=" + minTermFreq + ", minWordLength=" + minWordLength + ", maxTerms=" + maxTerms
                + ", interstingTerms=" + interstingTerms + ", filterQuery=" + filterQuery + "]";
    }
    
    
}
