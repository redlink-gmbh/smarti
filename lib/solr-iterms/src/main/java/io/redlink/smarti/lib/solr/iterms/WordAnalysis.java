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

    @Override
    public String toString() {
        return "WordAnalysis [word=" + word + ", term=" + term + ", start=" + start + ", end=" + end + ", pos=" + pos + "]";
    }
    
    
}