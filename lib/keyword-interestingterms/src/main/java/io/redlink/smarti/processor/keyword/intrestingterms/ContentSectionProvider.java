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

package io.redlink.smarti.processor.keyword.intrestingterms;

import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.Span;

import java.util.*;

public interface ContentSectionProvider {
    
    Iterator<Span> getContents(AnalyzedText at);
    
    /**
     * Default implementation of this interface that returns an Iterator
     * over a single element holding {@link AnalyzedText}
     */
    public static final ContentSectionProvider DEFAULT = new ContentSectionProvider() {
        
        @Override
        public Iterator<Span> getContents(AnalyzedText at) {
            return Collections.<Span>singleton(at).iterator();
        }
        
    };
    
    public static final ContentSectionProvider SECTIONS = new ContentSectionProvider() {
        
        @Override
        public Iterator<Span> getContents(AnalyzedText at) {
            final Iterator<Section> sections = at.getSections();
            if(sections.hasNext()){
                return new Iterator<Span>() {

                    //set holding sections without sub-sections
                    List<Section> contexts = new LinkedList<>();
                    //the current path to the root
                    Set<Section> contentSections = new HashSet<>();
                    Section next = null;
                    
                    @Override
                    public boolean hasNext() {
                        return getNextSection() != null;
                    }
                    
                    private Section getNextSection(){ //TODO: continue
                        if(next != null){
                            return next;
                        }
                        while(next == null && sections.hasNext()){
                            Section section = sections.next();
                            contentSections.add(section);
                            Section cSection = null;
                            ListIterator<Section> cit = contexts.listIterator(contexts.size());
                            while(cit.hasPrevious()){ //
                                cSection = cit.previous();
                                if(section.getStart() >= cSection.getEnd()){ //none overlapping
                                    cit.remove();
                                    if(next == null && contentSections.remove(cSection)){
                                        //context has no sub sections
                                        next = cSection;
                                    }
                                } else { //this context has a sub-section
                                    contentSections.remove(cSection); 
                                }
                            }
                            contexts.add(section);
                        }
                        //even if their are no more section (sections.hasNext() == false)
                        //their is still a last content section in the context
                        if(next == null && !contexts.isEmpty()){
                            Section last = contexts.get(contexts.size()-1);
                            if(contentSections.contains(last)){
                                next = last;
                            }
                            contexts.clear(); //now we are done ... clean the context
                        }
                        return next;
                    }

                    @Override
                    public Span next() {
                        Section nextSection = getNextSection();
                        if(nextSection == null){
                            throw new NoSuchElementException();
                        } else {
                            next = null; //consume;
                            return nextSection;
                        }
                    }
                    
                };
            } else {
                return DEFAULT.getContents(at);
            }
        }
    };

}
