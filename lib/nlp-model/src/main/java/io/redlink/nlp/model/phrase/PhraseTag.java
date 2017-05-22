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
package io.redlink.nlp.model.phrase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.redlink.nlp.model.pos.LexicalCategory;
import io.redlink.nlp.model.tag.Tag;
import io.redlink.nlp.model.tag.TagSet;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;

import java.util.*;

public class PhraseTag extends Tag<PhraseTag>{

    private final EnumSet<PhraseCategory> categories;

    /**
     * Creates a new Phrase tag for the parsed tag. The created Tag is not
     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
     * by components that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag){
        this(tag,(PhraseCategory) null);
    }
    /**
     * Creates a new Phrase tag for the parsed tag. The created Tag is not
     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
     * by components that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @param coordinated if this phrase is coordinated
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag, boolean coordinated){
        this(tag, null, coordinated);
    }
    /**
     * Creates a PhraseTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag,PhraseCategory category){
        this(tag, category == null ? null : Collections.singleton(category));
    }
    /**
     * Creates a PhraseTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @param coordinated if this phrase is coordinated
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag, PhraseCategory category, boolean coordinated){
        this(tag, category == null ? null : Collections.singleton(category));
        if(coordinated){
            categories.add(PhraseCategory.Coordination);
        } else {
            categories.remove(PhraseCategory.Coordination);
        }
    }
    /**
     * Creates a PhraseTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @param coordinated if this phrase is coordinated
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag, PhraseCategory...categories){
        this(tag, categories == null ? null : Arrays.asList(categories));
    }
    /**
     * Creates a PhraseTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @param coordinated if this phrase is coordinated
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    @PersistenceConstructor
    public PhraseTag(String tag, Collection<PhraseCategory> categories){
        super(tag);
        this.categories = EnumSet.noneOf(PhraseCategory.class);
        if(categories != null){
            for(PhraseCategory pc : categories){
                this.categories.add(pc);
                this.categories.addAll(pc.hierarchy());
            }
        }
    }
    /**
     * The {@link PhraseCategory PhraseCategories} of this tag (if known)
     * @return the categories or an empty set if not mapped to any. The
     * returned Set will also contain parent categories.
     */
    public Set<PhraseCategory> getCategories(){
       return categories; 
    }
    
    /**
     * If this Phrase is coordinated
     * @return the coordinated state
     */
    @Transient
    @JsonIgnore
    public boolean isCoordinated() {
        return categories.contains(PhraseCategory.Coordination);
    }
    
    @Override
    public String toString() {
        return String.format("Phrase %s (%s)", tag,
            categories.isEmpty() ? "none" : categories);
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof PhraseTag &&
                categories.equals(((PhraseTag)obj).categories);
    }
    
    
}
