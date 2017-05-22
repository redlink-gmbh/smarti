package io.redlink.nlp.model.pos;

import java.util.*;

/**
 * Allows to configure a set of POS tags.
 * @author Rupert Westenthaler
 *
 */
public class PosSet {

    /**
     * All Nouns including {@link Pos#Foreign} and {@link Pos#Abbreviation}s
     */
    public static final PosSet NOUNS = new PosSet(EnumSet.of(LexicalCategory.Noun), 
                EnumSet.of(Pos.Foreign, Pos.Abbreviation), Collections.emptySet());
    
    /**
     * All Verbs
     */
    public static final PosSet VERBS = new PosSet(EnumSet.of(LexicalCategory.Verb), 
                EnumSet.noneOf(Pos.class), Collections.emptySet());

    /**
     * All Adjectives
     */
    public static final PosSet ADJECTIVES = new PosSet(EnumSet.of(LexicalCategory.Adjective), 
                EnumSet.noneOf(Pos.class), Collections.emptySet());
    
    /**
     * All Tokens indicating a negation
     */
    public static final PosSet NEGATION = new PosSet(EnumSet.noneOf(LexicalCategory.class), 
                EnumSet.of(Pos.NegativeAdverb, Pos.NegativeDeterminer, Pos.NegativeParticle, Pos.NegativePronoun,
                        Pos.SubordinatingConjunctionWithNegation),
                Collections.emptySet());
    
    private Set<LexicalCategory> cats;
    private Set<Pos> posTags;
    private Set<String> tags;
    
    protected PosSet(Set<LexicalCategory> cats, Set<Pos> posTags, Set<String> tags){
        this.cats = cats == null ? EnumSet.noneOf(LexicalCategory.class) : cats;
        this.posTags = posTags == null ? EnumSet.noneOf(Pos.class) : posTags;
        this.tags = tags == null ? new HashSet<String>() : tags;
    }
    
    public static PosSet empty(){
        return new PosSet(null,null,null);
    }
    
    public static PosSet of(LexicalCategory...lc){
        return new PosSet(lc == null ? null : EnumSet.copyOf(Arrays.asList(lc)),null,null);
    }
    public static PosSet of(Pos...pos){
        return new PosSet(null,pos == null ? null : EnumSet.copyOf(Arrays.asList(pos)),null);
    }
    public static PosSet of(String...tags){
        return new PosSet(null, null, tags == null ? null : new HashSet<String>(Arrays.asList(tags)));
    }
    
    public Set<LexicalCategory> getCategories() {
        return cats;
    }
    
    public Set<Pos> getPosTags() {
        return posTags;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    public PosSet add(LexicalCategory...cats) {
        if(cats != null){
            this.cats.addAll(Arrays.asList(cats));
        }
        return this;
    }
    
    public PosSet add(Pos...pos) {
        if(pos != null){
            this.posTags.addAll(Arrays.asList(pos));
        }
        return this;
    }

    public PosSet add(String...tags) {
        if(tags != null){
            this.tags.addAll(Arrays.asList(tags));
        }
        return this;
    }

    public static PosSet union(PosSet...posSets) {
        return new PosSet(null, null, null).add(posSets);
    }

    public PosSet add(PosSet...posSets) {
        if(posSets != null && posSets.length > 0){
            for(int i=0;i<posSets.length;i++){
                if(posSets[i] != null){
                    this.cats.addAll(posSets[i].cats);
                    this.posTags.addAll(posSets[i].posTags);
                    this.tags.addAll(posSets[i].tags);
                }
            }
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cats == null) ? 0 : cats.hashCode());
        result = prime * result + ((posTags == null) ? 0 : posTags.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
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
        PosSet other = (PosSet) obj;
        if (cats == null) {
            if (other.cats != null)
                return false;
        } else if (!cats.equals(other.cats))
            return false;
        if (posTags == null) {
            if (other.posTags != null)
                return false;
        } else if (!posTags.equals(other.posTags))
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PosSet [cats=" + cats + ", posTags=" + posTags + ", tags=" + tags + "]";
    }

    
    
}
