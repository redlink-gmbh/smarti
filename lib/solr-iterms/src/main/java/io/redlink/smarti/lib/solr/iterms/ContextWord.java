package io.redlink.smarti.lib.solr.iterms;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class ContextWord extends ContextTerm {

    private final Collection<WordAnalysis> wordAnalysis;
    
    public ContextWord(ContextTerm ctxTerm, Collection<WordAnalysis> wordAnalysis) {
        super(ctxTerm.getField(),ctxTerm.getTerm(), ctxTerm.getRelevance());
        this.wordAnalysis = wordAnalysis;
    }
    
    public Set<String> getWords(){
        return wordAnalysis.stream().map(WordAnalysis::getWord).collect(Collectors.toSet());
    }
    
    public Collection<WordAnalysis> getWordAnalysis() {
        return wordAnalysis;
    }
    
    public String getWord() {
        return wordAnalysis.stream()
                .sorted(Comparator.comparing(WordAnalysis::getPos)) //use the first mention in the text
                .map(WordAnalysis::getWord)
                .findFirst()
                .orElse(null);
    }
    
}
