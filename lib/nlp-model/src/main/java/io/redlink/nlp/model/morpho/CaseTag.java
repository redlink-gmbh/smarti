/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.redlink.nlp.model.morpho;

import io.redlink.nlp.model.Token;
import io.redlink.nlp.model.tag.Tag;
import io.redlink.nlp.model.tag.TagSet;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * An Case tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class CaseTag extends Tag<CaseTag>{
    
    private final Case caseCategory;
    /**
     * Creates a new Case tag for the parsed tag. The created Tag is not
     * assigned to any {@link Case}.<p> This constructor can be used
     * by components that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public CaseTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a CaseTag that is assigned to a {@link Case}
     * @param tag the tag
     * @param caseCat the lexical case or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    @PersistenceConstructor
    public CaseTag(String tag,Case caseCat){
        super(tag);
        this.caseCategory = caseCat;
    }
    /**
     * The case of this tag (if known)
     * @return the case or <code>null</code> if not mapped to any
     */
    public Case getCase(){
       return this.caseCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("CASE %s (%s)", tag,
        	caseCategory == null ? "none" : caseCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof CaseTag &&
            (caseCategory == null && ((CaseTag)obj).caseCategory == null) ||
                    (caseCategory != null && caseCategory.equals(((CaseTag)obj).caseCategory));
    }
    
}