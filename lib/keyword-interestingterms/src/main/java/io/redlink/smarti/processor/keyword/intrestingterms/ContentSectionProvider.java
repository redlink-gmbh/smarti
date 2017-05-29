package io.redlink.smarti.processor.keyword.intrestingterms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.Span;

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
