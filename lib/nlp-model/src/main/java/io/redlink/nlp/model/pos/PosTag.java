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
package io.redlink.nlp.model.pos;

import io.redlink.nlp.model.Annotations;
import io.redlink.nlp.model.Token;
import io.redlink.nlp.model.tag.Tag;
import io.redlink.nlp.model.tag.TagSet;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An POS (part-of-speech) tag typically assigned by an POS-Tagger (an
 * NLP component) to a {@link Token} by using the {@link Annotations#POS_ANNOTATION}<p>
 * The only required field is {@link #getTag()} - the string tag assigned by
 * the POS Tagger.<p>
 * PosTags can be mapped to a {@link LexicalCategory} and be part of an
 * {@link TagSet}. 
 * NOTE that the {@link TagSet} is set by the {@link TagSet#addTag(Tag)} method.<p>
 */
public class PosTag extends Tag<PosTag>{
    
    /**
     * The {@link LexicalCategory LexicalCategories} as parsed in the constructor.
     * <code>null</code> if none are parsed
     */
    private final Set<LexicalCategory> category;
    
    /**
     * The {@link LexicalCategory LexicalCategories} applying to this PosTag. This
     * also includes LexicalCategories of parsed {@link Pos} tags
     */
    @Transient
    private final Set<LexicalCategory> categories;
    /**
     * The {@link Pos} tags as parsed in the constructor. 
     * <code>null</code> if none are parsed
     */
    private final Set<Pos> pos;
    /**
     * The {@link Pos} hierarchy for all {@link Pos} tags parsed in the 
     * constructor. NOTE: NULL if {@link #pos} is empty!
     */
    @Transient
    private final Set<Pos> posHierarchy;
    /**
     * Creates a new POS tag for the parsed tag. The created Tag is not
     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
     * by components that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag){
        this(tag, null, null, false);
    }
    /**
     * Creates a PosTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param first the first lexical category
     * @param rest the rest of assigned lexical categories
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag,LexicalCategory first, LexicalCategory...rest){
        this(tag, EnumSet.of(first, rest), null, false);
    }
    /**
     * Creates a PosTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param pos a concrete {@link Pos} mapped to the string
     * @param furtherPos allows to add additional {@link Pos} mappings
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag,Pos pos, Pos...furtherPos){
        this(tag, null, EnumSet.of(pos,furtherPos), false);
    }
    
    public PosTag(String tag,LexicalCategory category, Pos pos,Pos...furtherPos){
        this(tag,category == null ? null : EnumSet.of(category),
                EnumSet.of(pos,furtherPos),false);
    }
    @PersistenceConstructor
    public PosTag(String tag,Collection<LexicalCategory> category, Collection<Pos> pos){
        this(tag,category,pos,true);
    }
    public PosTag(String tag,Collection<LexicalCategory> category, Collection<Pos> pos, boolean copy){
        super(tag);
        if(copy){
            if(pos != null && !pos.isEmpty()){
                this.pos = EnumSet.copyOf(pos);
            } else {
                this.pos = null;
            }
        } else {
            this.pos = (EnumSet<Pos>)pos;
        }
        if(copy){
            if(category != null && !category.isEmpty()){
                this.category = EnumSet.copyOf(category);
            } else {
                this.category = null;
            }
        } else {
            this.category = (EnumSet<LexicalCategory>)category;
        }
        //and the union over the pos parents
        this.categories = this.category == null ? EnumSet.noneOf(LexicalCategory.class) : EnumSet.copyOf(category);
        this.posHierarchy = EnumSet.noneOf(Pos.class);
        if(this.pos != null){
            for(Pos p : this.pos){
                this.posHierarchy.addAll(p.hierarchy());
                this.categories.addAll(p.categories());
            }
        }
    }
    /**
     * The {@link LexicalCategory LexicalCategories} of this tag
     * @return the {@link LexicalCategory LexicalCategories} or an
     * empty {@link Set} if the string {@link #getTag() tag} is 
     * not mapped.
     */
    public Set<LexicalCategory> getCategories(){
       return categories; 
    }
    
    /**
     * Checks if this {@link PosTag} is mapped to the parsed
     * {@link LexicalCategory}
     * @param category the category
     * @return <code>true</code> if this PosTag is mapped to
     * the parsed category.
     */
    public boolean hasCategory(LexicalCategory category){
        return this.categories.contains(category);
    }
    
    /**
     * Checks if the {@link PosTag} is of the parsed {@link Pos}
     * tag. This also considers the transitive hierarchy of
     * the {@link Pos} enum.
     * @param pos the {@link Pos} to check
     * @return <code>true</code> if this PosTag is mapped to
     * the parsed {@link Pos}.
     */
    public boolean hasPos(Pos pos){
        return this.pos == null ? false : 
            posHierarchy.contains(pos);
    }
    /**
     * Returns <code>true</code> if this PosTag is mapped to a
     * {@link LexicalCategory} or a {@link Pos} type as defined
     * by the Olia Ontology
     * @return the state
     */
    public boolean isMapped() {
        return !categories.isEmpty();
    }
    
    /**
     * Getter for the {@link Pos} mapped to this PosTag
     * @return the mapped {@link Pos} mapped to the string
     * string {@link #getTag() tag} or an empty set of not
     * mapped. This are the directly mapped {@link Pos} types
     * and does not include the parent Pos types.
     */
    public Set<Pos> getPos() {
        return pos == null ? Collections.emptySet() : pos;
    }
    
    public Set<Pos> getPosHierarchy(){
        return posHierarchy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("pos: ");
        sb.append(tag);
        if(pos != null || !categories.isEmpty()){
            sb.append('(');
            if(pos != null && !pos.isEmpty()){
                if(pos.size() == 1){
                    sb.append(pos.iterator().next());//.name());
                } else {
                    sb.append(pos);
                }
                sb.append('|');
            }
            if(categories.size() == 1){
                sb.append(categories.iterator().next());//.name());
            } else {
                sb.append(categories);
            }
            sb.append(')');
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + ((pos == null) ? 0 : pos.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        PosTag other = (PosTag) obj;
        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;
        if (pos == null) {
            if (other.pos != null)
                return false;
        } else if (!pos.equals(other.pos))
            return false;
        return true;
    }

    
    
}
