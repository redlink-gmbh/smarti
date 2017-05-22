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

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Am analyzed text - text with sections that can all be annotated.
 * @author Rupert Westenthaler
 *
 */
public class AnalyzedText extends SpanCollection implements Iterable<Span>{

        
    /**
     * The text
     */
    @Transient //the text is saved in Document#getContent()
    private String text;
    @Transient //the spans collection is used instead
    protected NavigableMap<Span,Span> spansMap = new TreeMap<Span,Span>();
    
    @AccessType(Type.PROPERTY)
    private Collection<Span> spans; //not used just to make spring data happy
    
    @PersistenceConstructor
    protected AnalyzedText(int[] span, Collection<Span> spans) {
       super(SpanTypeEnum.Text, span);
       this.context = this; //set itself as context
       if(spans != null){
           for(Span s : spans){
               register(s);
           }
       }
    }
    
    public AnalyzedText(int length){
        super(SpanTypeEnum.Text,new int[]{0,length});
        this.setContext(this); //the the context to itself
    }

    public AnalyzedText(String text){
        super(SpanTypeEnum.Text, new int[] {0,text.length()});
        this.setContext(this); //the the context to itself
        this.text = text;
    }
    
    /**
     * Creates an {@link AnalyzedText} by building up content section
     * @return the {@link AnalyzedTextBuilder}
     */
    public static AnalyzedTextBuilder build(){
        return new AnalyzedTextBuilder();
    }
    
    /**
     * Returns {@link SpanTypeEnum#Text}
     * @see Span#getType()
     * @see SpanTypeEnum#Text
     */
    public SpanTypeEnum getType() {
        return SpanTypeEnum.Text;
    }
    
    /**
     * Adds an Section
     * @param start the start index
     * @param end the end index
     * @return the Section
     */
    public Section addSection(int start, int end){
        return register(new Section(context, this, start, end));
    }
    /**
     * All sections of the Analyzed texts.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return An iterator over the sections
     */
    public Iterator<Section> getSections(){
        return filter(Section.class);
    }

    /**
     * Adds an Sentence
     * @param start the start index
     * @param end the end index
     * @return the Sentence
     */
    public Sentence addSentence(int start, int end){
        return register(new Sentence(context, this, start, end));
    }

    /**
     * All sentences of the Analysed texts.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return An iterator over the sentences
     */
    public Iterator<Sentence> getSentences(){
        return filter(Sentence.class);
    }
        
    /**
     * Adds an Chunk
     * @param start the start of the chunk
     * @param end the end of the chunk
     * @return the chunk for the parsed start/end position
     */
    public Chunk addChunk(int start, int end){
        return register(new Chunk(context, this, start, end));
    }
    
    /**
     * All Chunks of this analysed text.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    public Iterator<Chunk> getChunks(){
        return filter(Chunk.class);
    }
    
    /**
     * Allows to set the text if not directly parsed to the constructor. The
     * parsed text MUST NOT be NULL and MUST have the same length as the
     * {@link #getEnd()} of this AnalyzedText. The text can only set once. If
     * already set (or parsed with the constructor) an {@link IllegalStateException}
     * will be thrown
     * @param text the text
     * @throws IllegalArgumentException if the parsed text is <code>null</code> or
     * the {@link String#length()} != {@link #getEnd()}
     * @throws IllegalStateException if the {@link #getText()} is already set
     * (<code>{@link #hasText()} == true</code>)
     */
    public void setText(String text){
        if(text == null){
            throw new IllegalArgumentException("The parsed Text MUST NOT be NULL!");
        }
        if(text.length() != getEnd()){
            throw new IllegalArgumentException("The length of the parsed Text MUST be the "
                    + "same as the end of the AnalyzedText span (span: [0, "+getEnd()+"], "
                    + "text length: "+ text.length() +")!");
        }
        if(hasText()){
            throw new IllegalStateException("The text for an AnalyzedText can only be set once!");
        }
        this.text = text;
    }
    
    /**
     * If the plain text content for this AnalyzedText is available
     * @return the state
     */
    public boolean hasText(){
        return text != null;
    }
    
    /**
     * Getter for the text.
     * @return the text
     */
    public CharSequence getText() {
        return text;
    }
    
    /**
     * Getter for the unmodifiable collection of Spans. Used for persistence
     * @return The unmodifiable collection of spans
     */
    @AccessType(Type.PROPERTY)
    public Collection<Span> getSpans(){
        return Collections.unmodifiableCollection(spansMap.values());
    }
    
    /**
     * Unmodifiable Iterator over all {@link Span}s
     * @return an Iterator over all spans
     */
    @Override
    public Iterator<Span> iterator() {
        return getSpans().iterator();
    }
    /**
     * The number of spans part of this analyzed text
     * @return
     */
    public int size(){
        return spansMap.size();
    }
    
    /**
     * Allows to build up an {@link AnalyzedText} while parsing the Content
     * (e.g. with Tika by processing SAX events for the different sections
     * @author Rupert Westenthaler
     */
    public static class AnalyzedTextBuilder extends Writer {
        
        /**
         * temporarily used to collect sections
         */
        private Collection<Section> sections = new LinkedList<>();
        private StringBuilder textBuilder = new StringBuilder();
        
        private AnalyzedTextBuilder(){
        }
        
        /**
         * Appends a section to the text.
         * @param prefix the prefix - not part of the section (e.g. line breaks to
         * be added before the section)
         * @param sectionText the text of the section
         * @param suffix the suffix - not part of the section (e.g. line breaks
         * after the section)
         * @return the section (e.g. to {@link Section#addAnnotation(Annotation, Value) add annotations})
         * @throws IllegalArgumentException if the sectionText is <code>null</code> or empty
         */
        public Section appendSection(String prefix, String sectionText, String suffix){
            if(StringUtils.isEmpty(sectionText)){
                throw new IllegalArgumentException("The parsed sectionText MUST NOT be NULL nor empty!");
            }
            if(prefix != null){
                textBuilder.append(prefix);
            }
            int start = textBuilder.length();
            textBuilder.append(sectionText);
            int end = textBuilder.length();
            Section section = new Section(new int[]{start, end});
            sections.add(section);
            if(suffix != null){
                textBuilder.append(suffix);
            }
            return section;
        }
        /**
         * Defines a section within an already appended section of the text.
         * As an example one might want to add paragraphs while also adding
         * sections for pages as soon as a paragraphs continues on the next page. 
         * @param start the start ofset of the section
         * @param end the end ofset of the section
         * @return the section (e.g. to {@link Section#addAnnotation(Annotation, Value) add annotations})
         * @throws IllegalArgumentException if <code>start &lt; 0</code> or 
         * <code>start &gt;= end</code> or the end exeeds the current length of the text
         * @see #defineTextSection(int, int)
         */
        public Section defineSection(int start, int end){
            if(end <= start || start < 0){
                throw new IllegalArgumentException("Invalid section [" + start
                        + "," + end + "] parsed!");
            }
            if(end > textBuilder.length()){
                throw new IllegalArgumentException("parsed section  [" + start
                        + "," + end + "] exeeds the current length '"
                        + textBuilder.length() + "'of the text");
            }
            Section section = new Section(new int[]{start, end});
            sections.add(section);
            return section;
        }
        
        /**
         * Defines a section based on the parsed start/end indexes by cutting 
         * any {@link Character#isWhitespace(int) whitespace char} from the start
         * end end index.
         * @param start the start position
         * @param end the end position
         * @return the text section starting at the first none whitespace code point and
         * ending at the last none whitespace code point within the parsed section.
         * Returns <code>null</code> if the parsed section only consists of 
         * whitespace chars.
         * @throws IllegalArgumentException if <code>start &lt; 0</code> or 
         * <code>start &gt;= end</code> or the end exeeds the current length of the text
         * @see #defineSection(int, int)
         */
        public Section defineTextSection(int start, int end){
            if(end <= start || start < 0){
                throw new IllegalArgumentException("Invalid section [" + start
                        + "," + end + "] parsed!");
            }
            if(end > textBuilder.length()){
                throw new IllegalArgumentException("parsed section  [" + start
                        + "," + end + "] exeeds the current length '"
                        + textBuilder.length() + "'of the text");
            }
            int idx = start;
            int tStart = -1;
            int tEnd = -1;
            while(idx < end && tStart == -1){
                if(!Character.isWhitespace(textBuilder.codePointAt(idx))){
//              if(Character.isAlphabetic(textBuilder.codePointAt(idx))){
                    tStart = idx;
                }
                idx++;
            }
            if(tStart == -1){
                return null; //no text section found
            }
            idx = end;
            while(idx > tStart && tEnd == -1){
                if(!Character.isWhitespace(textBuilder.codePointAt(idx-1))){
//                if(Character.isAlphabetic(textBuilder.codePointAt(idx-1))){
                    tEnd = idx;
                }
                idx--;
            }
            if(tEnd == -1){
                return null;
            }
            return defineSection(tStart, tEnd);
        }
        
        /**
         * Getter for the size of the currently contained text.
         * @return the character count of the currently contained text
         */
        public int size(){
            return textBuilder.length();
        }
        /**
         * Allows access to the {@link StringBuilder} used to collect text.
         * @return the StringBuilder
         */
        public StringBuilder getStringBuilder(){
            return textBuilder;
        }
        /**
         * Write a portion of an array of characters.
         *
         * @param  cbuf  Array of characters
         * @param  off   Offset from which to start writing characters
         * @param  len   Number of characters to write
         */
        public void write(char cbuf[], int off, int len) {
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            textBuilder.append(cbuf, off, len);
        }
        /**
         * Write a single character.
         */
        public void write(int c) {
            textBuilder.append((char) c);
        }

        /**
         * Write a string.
         */
        public void write(String str) {
            textBuilder.append(str);
        }

        /**
         * Write a portion of a string.
         *
         * @param  str  String to be written
         * @param  off  Offset from which to start writing characters
         * @param  len  Number of characters to write
         */
        public void write(String str, int off, int len)  {
            textBuilder.append(str.substring(off, off + len));
        }

        /**
         * Appends a char sequence
         * @param csq the char sequence to add
         * @return this
         */
        public AnalyzedTextBuilder append(CharSequence csq) {
            if (csq != null){
                write(csq.toString());
            }
            return this;
        }

        /**
         * Appends part of a char sequence
         * @param csq the char sequence to add
         * @param start the start offset
         * @param end the end offset
         * @return this
         */
        public AnalyzedTextBuilder append(CharSequence csq, int start, int end) {
            if(csq != null){
                write(csq.subSequence(start, end).toString());
            }
            return this;
        }

        /**
         * Appends a single char
         * @param c the char
         * @return this
         */
        public AnalyzedTextBuilder append(char c) {
            write(c);
            return this;
        }
        
        /**
         * Flush the stream.
         */
        public void flush() { /* no op */ }

        /**
         * Closing a <tt>StringWriter</tt> has no effect. The methods in this
         * class can be called after the stream has been closed without generating
         * an <tt>IOException</tt>.
         */
        public void close() throws IOException { /* no op */ }
        
        /**
         * Creates the AnalyzedText instance for the current state of the builder
         * @return the {@link AnalyzedText} instance
         */
        public AnalyzedText create(){
            AnalyzedText at = new AnalyzedText(textBuilder.toString());
            for(Section section: sections){
                at.register(section);
            }
            return at;
        }
        
    }

}
