package io.redlink.nlp.model.keyword;

import org.springframework.data.annotation.PersistenceConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides all information about an extracted keyword needed to write the
 * KeywordAnnotation
 * 
 * TODO: check of {@link #getContained()} is a good Idea, or if we should use
 * a flat list of keywords with references to contained one instead
 * 
 * @author Rupert Westenthaler
 *
 */
public class Keyword {
    
    private final String key;
    private final String keyword;
    private String cleanedKeyword;
    private double metric;
    private double count;
    
    private Set<String> contained = new HashSet<>();
    
    @PersistenceConstructor
    public Keyword(String key, String keyword) {
        assert key != null;
        this.key = key;
        this.keyword = keyword;
    }
    
    public String getKey() {
        return key;
    }

    public void setMetric(double metric) {
        this.metric = metric;
    }
    
    public double getMetric() {
        return metric;
    }
    
    public void setCount(double count) {
        this.count = count;
    }
    
    public double getCount() {
        return count;
    }
    /**
     * Getter for the keyword as originally extracted.
     * @return the (uncleaned) keyword.
     */
    public String getOriginalKeyword(){
        return keyword;
    }
    /**
     * The (possible cleaned) textual representation of the keyword
     * @return the textual representation of the keyword
     */
    public String getKeyword(){
        return cleanedKeyword == null ? keyword : cleanedKeyword;
    }
    /**
     * Allows to set the cleaned keyword
     * @param cleanedKeyword the cleaned keyword
     */
    public void setCleanedKeyword(String cleanedKeyword) {
        this.cleanedKeyword = cleanedKeyword;
    }
    
    public void addContained(String key){
        contained.add(key);
    }
    
    public void addAllContained(Collection<String> contained) {
        this.contained.addAll(contained);
    }
    
    public void setContained(Set<String> contained) {
        this.contained = contained;
    }
    
    /**
     * The key of (shorter) keywords contained in this one. The full keyword
     * for the key is expected to be present in the same list of keywords
     * @return The {@link Keyword#getKey() key}s of contained keywords
     */
    public Set<String> getContained() {
        return contained;
    }

    @Override
    public String toString() {
        return "Keyword [key=" + key + ", keyword=" + keyword + ", metric=" + metric + ", count=" + count + "]";
    }
    
}