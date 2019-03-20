package io.redlink.smarti.lib.solr.iterms;

public class WordAnalysis {
    
    public final String word;
    public final String term;
    public final int start;
    public final int end;
    public final int pos;
    
    public WordAnalysis(String word, String term, int start, int end, int pos) {
        this.word = word;
        this.term = term;
        this.start = start;
        this.end = end;
        this.pos = pos;
    }
    
    public String getWord() {
        return word;
    }
    
    public String getTerm() {
        return term;
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
    
    public int getPos() {
        return pos;
    }
}