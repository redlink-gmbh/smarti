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


public final class Sentence extends SpanCollection {

    @PersistenceConstructor
    protected Sentence(int[] span){
        super(SpanTypeEnum.Sentence, span);
    }

    protected Sentence(AnalyzedText at, Span relativeTo,int start, int end){
        super(at, SpanTypeEnum.Sentence, relativeTo, start, end);
    }
    
    /**
     * Adds an Chunk relative to this Sentence
     * @param start the start of the chunk relative to the sentence
     * @param end
     * @return
     */
    public Chunk addChunk(int start, int end){
        return register(new Chunk(context, this, start, end));
    }
    
    /**
     * The Chunks covered by this Sentence<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    public Iterator<Chunk> getChunks(){
        return filter(Chunk.class);
    }
}
