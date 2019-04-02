package io.redlink.smarti.lib.solr.iterms;

import org.apache.solr.client.solrj.util.ClientUtils;

public class ContextTerm {
    
    private final String field;
    private final String term;
    private final float relevance;
    
    public ContextTerm(String solrTerm, float relevance){
        int fieldSepIdx = solrTerm.indexOf(':'); //we need to strip the solr field from the term 
        //NOTE: we do not Solr query escape as we do not want to make assumptions about the
        //      implementation of the Rocket.Chat search
        if(fieldSepIdx >= 0){
            term = solrTerm.substring(fieldSepIdx + 1);
            field = solrTerm.substring(0,fieldSepIdx);
        } else {
            term = solrTerm;
            field = null;
        }
        this.relevance = relevance;
    }
    protected ContextTerm(String field, String term, float relevance) {
        this.field = field;
        this.term = term;
        this.relevance = relevance;
    }
    
    public String getField() {
        return field;
    }
    
    public String getTerm() {
        return term;
    }
    /**
     * The combination of <code>&lt;field&gt;:&lt;term&gt;</code> as used by Solr. 
     * Also escapes query chars from the term 
     * @return '<code>&lt;field&gt;:&lt;term&gt;</code>' or '<code>&lt;term&gt;</code>' if the field is <code>null</code>
     */ //#307: the term needs to be query escaped
    public String getSolrTerm() {
        return (field == null ? "" : field + ':') + ClientUtils.escapeQueryChars(term);
    }
    
    public float getRelevance() {
        return relevance;
    }
}