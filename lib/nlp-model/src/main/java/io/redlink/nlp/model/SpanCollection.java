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

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.InstanceofPredicate;

import java.util.*;


/**
 * A {@link Span} that may enclose other Spans. Super type for {@link Chunk}s,
 * {@link Sentence}s and {@link AnalyzedText}.<p>
 * As {@link Span} this is an meta (abstract) type. Implementations of this
 * Interface SHOULD BE abstract Classes. 
 */
public abstract class SpanCollection extends Span {

    /**
     * Allows to create a {@link SpanCollection} without setting the AnalysedText context.
     * {@link #setContext(AnalyzedText)} needs to be called before using
     * this instance.<p>
     * NOTE: this constructor is needed to instantiate {@link AnalyzedText}.
     * @param type the type. MUST NOT be <code>null</code> nor {@link SpanTypeEnum#Token}
     * @param span the span
     */
    protected SpanCollection(SpanTypeEnum type, int[] span) {
        super(type, span);
        assert type != SpanTypeEnum.Token : "The SpanType 'Token' is NOT a Section - can not cover other spans!";
    }
    
    /**
     * Creates a new {@link SpanCollection} within the context of an {@link AnalyzedText} instance
     * @param at the context
     * @param type the type. MUST NOT be <code>null</code> nor {@link SpanTypeEnum#Token}
     * @param relativeTo the span start and end offsets are relative to
     * @param start the start offset
     * @param end the end offset
     */
    protected SpanCollection(AnalyzedText at, SpanTypeEnum type, Span relativeTo, int start,int end) {
        super(at,type,relativeTo,start,end);
        assert type != SpanTypeEnum.Token : "The SpanType 'Token' is NOT a Section - can not cover other spans!";
    }

    
    /**
     * Iterates over all Span enclosed by this one that are of any of the
     * parsed Types.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @param types the {@link SpanTypeEnum types} of Spans included
     * @return sorted iterator over the selected Spans.
     */
    @SuppressWarnings("unchecked")
    public Iterator<Span> getEnclosed(final Set<SpanTypeEnum> types) {
        return IteratorUtils.filteredIterator(getIterator(), 
            new Predicate() {
                @Override
                public boolean evaluate(Object span) {
                    return types.contains(((Span)span).getType());
                }
            });
    }
    
    /**
     * Iterates over all enclosed Span within the parsed window. Only Spans
     * with on of the parsed types are returned. 
     * <p> 
     * The parsed window (start/end indexes) are relative to the section. If
     * the parsed window exceeds the Section the window adapted to the section.
     * This means that this method will never return Spans outside the section.
     * <p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @param types the {@link SpanTypeEnum types} of Spans included
     * @param startOffset the start offset relative to the start position of this {@link SpanCollection}
     * @param endOffset the end offset relative to the start position of this {@link SpanCollection}.
     * @return sorted iterator over the selected Spans.
     */
    @SuppressWarnings("unchecked")
    public Iterator<Span> getEnclosed(final Set<SpanTypeEnum> types, int startOffset, int endOffset) {
        if(startOffset >= (span[1] - span[0])){ //start is outside the span
            return Collections.<Span>emptySet().iterator();
        }
        int startIdx = startOffset < 0 ? span[0] : (span[0]+ startOffset);
        int endIdx = span[0] + endOffset;
        if(endIdx <= startIdx) {
            return Collections.<Span>emptySet().iterator();
        } else if(endIdx > span[1]){
            endIdx = span[1];
        }
        return IteratorUtils.filteredIterator(getIterator(new SubSetHelperSpan(startIdx, endIdx)), 
            new Predicate() {
                @Override
                public boolean evaluate(Object span) {
                    return types.contains(((Span)span).getType());
                }
            });
    }
    /**
     * Iterator that does not throw {@link ConcurrentModificationException} but
     * considers modifications to the underlying set by using the
     * {@link NavigableMap#higherKey(Object)} method for iterating over the
     * Elements!<p>
     * This allows to add new {@link Span}s to the {@link SpanCollection} while
     * iterating (e.g. add {@link Token}s and/or {@link Chunk}s while iterating
     * over the {@link Sentence}s of an {@link AnalyzedText})
     * @return the iterator
     */
    protected Iterator<Span> getIterator(){
        return getIterator(null);
    }
    /**
     * Iterator that does not throw {@link ConcurrentModificationException} but
     * considers modifications to the underlying set by using the
     * {@link NavigableMap#higherKey(Object)} method for iterating over the
     * Elements!<p>
     * This allows to add new {@link Span}s to the {@link SpanCollection} while
     * iterating (e.g. add {@link Token}s and/or {@link Chunk}s while iterating
     * over the {@link Sentence}s of an {@link AnalyzedText})
     * @param section the (sub-)section of the current section to iterate or
     * <code>null</code> to iterate the whole section.
     * @return the iterator
     */
    protected Iterator<Span> getIterator(final SubSetHelperSpan section){
        //create a virtual Span with the end of the section to iterate over
        final Span end = new SubSetHelperSpan(
            section == null ? getEnd() : //if no section is defined use the parent
                section.getEnd()); //use the end of the desired section
        return new Iterator<Span>() {
            
            boolean init = false;
            boolean removed = true;
            //init with the first span of the iterator
            private Span span = section == null ? 
                    SpanCollection.this : section; 
            
            @Override
            public boolean hasNext() {
                return getNext() != null;
            }
            
            private Span getNext(){
                Span next = context.spansMap.higherKey(span);
                return next == null || next.compareTo(end) >= 0 ? null : next;
            }
            
            @Override
            public Span next() {
                init = true;
                span = getNext();
                removed = false;
                if(span == null){
                    throw new NoSuchElementException();
                }
                return span;
            }

            @Override
            public void remove() {
                if(!init){
                    throw new IllegalStateException("remove can not be called before the first call to next");
                }
                if(removed){
                    throw new IllegalStateException("the current Span was already removed!");
                }
                context.spansMap.remove(span);
                removed = true;
            }
            
        };
    }
    
    /**
     * Adds a Token <b>relative</b> to the current Span. Negative values for start and
     * end are allowed (e.g. to add a Token that starts some characters before
     * this one.<p>
     * Users that want to use <b>absolute</b> indexes need to use
     * <pre>
     *     Span span; //any type of Span (Token, Chunk, Sentence ...)
     *     span.getContext().addToken(absoluteStart, absoluteEnd)
     * </pre>
     * @param start the start relative to this Span
     * @param end the end relative to this span
     * @return the created and added token
     */
    public Token addToken(int start,int end){
        return register(new Token(context, this,start, end));
    }
    /**
     * Registers the parsed - newly created token - with the {@link #getContext()}.
     * If the parsed {@link Span} already exists (an other Span instance with the
     * same values for {@link Span#getType()}, {@link Span#getStart()} and 
     * {@link Span#getEnd()}) than the already present instance is returned
     * instead of the parsed one. In case the parsed Token does not already
     * exist the parsed instance is registered with the context and
     * returned.<p>
     * Typical usage:<pre><code>
     *     public add{something}(int start, int end){
     *         return register(new {somthing}Impl(context, this,start,end));
     *     }
     * </code></pre>
     * {something} ... the Span type (Token, Chunk, Sentence ...)<p>
     * @param span the Span instance to register
     * @return the parsed or an already existing instance
     */
    protected <T extends Span> T register(T span){
        //check if this token already exists
        @SuppressWarnings("unchecked")
        T current = (T)context.spansMap.get(span);
        //NOTE: type safety is ensured by the SpanTypeEnum in combination with the
        //      Compareable implementation of SpanImpl.
        if(current == null){ //add the new one
            context.spansMap.put(span, span);
            span.context = context; //set this as context for the parsed span
            return span;
        } else { //else return the already contained token
            //copy already existing annotations (if any)
            for(String key : span.getKeys()){
                current.addValues(key, span.getValues(key));
            }
            return current;
        }
    }
    /**
     * Getter for the Iterator over all Tokens part of this SpanCollection
     * @return An iterator over all tokens part of this span collection
     */
    public Iterator<Token> getTokens(){
        return filter(Token.class);
    }
    /**
     * Internal helper to generate correctly generic typed {@link Iterator}s for
     * filtered {@link Span} types
     * @param clazz the actual Span implementation e.g. {@link Token}
     * @return the {@link Iterator} of type {interface} iterating over 
     * {implementation} instances (e.g. 
     * <code>{@link Iterator}&lt;{@link Token}&gt;</code> returning 
     * <code>{@link Token}</code> instances on calls to {@link Iterator#next()}
     */
    @SuppressWarnings("unchecked")
    protected <T extends Span> Iterator<T> filter(final Class<T> clazz){
        return IteratorUtils.filteredIterator(
            getIterator(),
            new InstanceofPredicate(clazz));
    }
    
    /**
     * Internal helper class used for building {@link SortedSet#subSet(Object, Object)}.
     * 
     * @author Rupert Westenthaler
     *
     */
    class SubSetHelperSpan extends Span {
        /**
         * Create the start constraint for {@link SortedSet#subSet(Object, Object)}
         * @param start
         * @param end
         */
        protected SubSetHelperSpan(int start,int end){
            super(SpanTypeEnum.Text, //lowest pos type
                new int[]{start, end});
            setContext(SpanCollection.this.context);
        }
        /**
         * Creates the end constraint for {@link SortedSet#subSet(Object, Object)}
         * @param pos
         */
        protected SubSetHelperSpan(int pos){
            super(SpanTypeEnum.Token, //highest pos type,
                new int[] {pos, Integer.MAX_VALUE});
            setContext(SpanCollection.this.context);
        }
    }

}
