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
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((term == null) ? 0 : term.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContextTerm other = (ContextTerm) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ContextTerm [term=" + field + ":" + term + ", relevance=" + relevance + "]";
    }
    
    
}