package io.redlink.smarti.processor.keyword.intrestingterms;

public class InterestingTerm {

    private final String source;
    private final String term;

    protected InterestingTerm(String source, String term) {
        this.source = source;
        this.term = term;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getTerm() {
        return term;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
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
        InterestingTerm other = (InterestingTerm) obj;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
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
        return "InterestingTerm [source=" + source + ", term=" + term + "]";
    }
    
    

}
