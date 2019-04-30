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

    @Override
    public String toString() {
        return "ContextWord [term: " + getSolrTerm() + "relevance=" + getRelevance() + ", words=" + getWords() + "]";
    }
    
}
