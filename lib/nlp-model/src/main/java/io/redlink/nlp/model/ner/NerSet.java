package io.redlink.nlp.model.ner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows to configure a set of POS tags.
 * @author Rupert Westenthaler
 *
 */
public class NerSet {

    /**
     * All Locations
     */
    public static final NerSet LOCATION = NerSet.ofType(NerTag.NAMED_ENTITY_LOCATION);
    
    /**
     * All Persons
     */
    public static final NerSet PERSON = NerSet.ofType(NerTag.NAMED_ENTITY_PERSON);

    /**
     * All Organizations
     */
    public static final NerSet ORGANIZATION = NerSet.ofType(NerTag.NAMED_ENTITY_ORGANIZATION);
    
    /**
     * All Named Entities with unknown type
     */
    public static final NerSet UNKNOWN = NerSet.ofType(NerTag.NAMED_ENTITY_UNKOWN);
    
    private Set<String> types;
    private Set<String> tags;
    
    protected NerSet(Collection<String> types, Collection<String> tags){
        this.types = new HashSet<>();
        if(types != null){
            this.types.addAll(types);
        }
        this.tags = new HashSet<>();
        if(tags != null){
            this.tags.addAll(tags);
        }
    }
    
    public static NerSet empty(){
        return new NerSet(null,null);
    }
    
    public static NerSet ofType(String...types){
        return new NerSet(types == null ? null : Arrays.asList(types), null);
    }
    public static NerSet ofTag(String...tags){
        return new NerSet(null, tags == null ? null : Arrays.asList(tags));
    }
    
    public Set<String> getTypes() {
        return types;
    }
    
    public Set<String> getTags() {
        return tags;
    }

    public NerSet addType(String...type) {
        if(type != null){
            this.types.addAll(Arrays.asList(type));
        }
        return this;
    }
    
    public NerSet addTag(String...tags) {
        if(tags != null){
            this.tags.addAll(Arrays.asList(tags));
        }
        return this;
    }

    public static NerSet union(NerSet...nerSets) {
        return new NerSet(null, null).add(nerSets);
    }

    public NerSet add(NerSet...nerSets) {
        if(nerSets != null && nerSets.length > 0){
            for(int i=0;i<nerSets.length;i++){
                if(nerSets[i] != null){
                    this.types.addAll(nerSets[i].types);
                    this.tags.addAll(nerSets[i].tags);
                }
            }
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((types == null) ? 0 : types.hashCode());
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
        NerSet other = (NerSet) obj;
        if (types == null) {
            if (other.types != null)
                return false;
        } else if (!types.equals(other.types))
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
        return "NerSet [types=" + types + ", tags=" + tags + "]";
    }
    
}