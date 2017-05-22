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
package io.redlink.nlp.model;

import org.springframework.data.annotation.PersistenceConstructor;

import java.util.ConcurrentModificationException;
import java.util.Iterator;


public final class Section extends SpanCollection {

    /**
     * Allows to create a SectionImpl without setting the AnalysedText context.
     * {@link #setContext(AnalyzedText)} needs to be called before using
     * this instance.<p>
     * NOTE: this constructor is needed to instantiate {@link AnalyzedText}.
     * @param span the span <code>[start, end]</code>
     */
    @PersistenceConstructor
    protected Section(int[] span) {
        super(SpanTypeEnum.TextSection, span);
    }

    /**
     * Creates a {@link SpanTypeEnum#TextSection Section of the Text} such as
     * a paragraph, an Heading, a Chapter, a Page ...
     * @param at the {@link AnalyzedText}
     * @param relativeTo the {@link Span} the offsets are relative to
     * @param start the start offset
     * @param end the end offset
     */
    protected Section(AnalyzedText at, Span relativeTo,int start, int end){
        super(at,SpanTypeEnum.TextSection,relativeTo,start,end);
    }
     
    
    
    /**
     * Adds an Sentence relative to this text section
     * @param start the start of the chunk relative to the sentence
     * @param end the end offset relative to the sentence
     * @return the sentence
     */
    public Sentence addSentence(int start, int end){
        return register(new Sentence(context, this, start, end));
    }
    
    /**
     * The Sentences covered by this text section<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the sentences
     */
    public Iterator<Sentence> getSentences(){
        return filter(Sentence.class);
    }
    /**
     * The Chunks covered by this text section<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    public Iterator<Chunk> getChunks(){
        return filter(Chunk.class);
    }
}
