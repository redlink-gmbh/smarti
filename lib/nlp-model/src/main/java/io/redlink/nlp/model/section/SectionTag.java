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
package io.redlink.nlp.model.section;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.redlink.nlp.model.tag.Tag;

public class SectionTag extends Tag<SectionTag>{

    private final SectionType section;

    /**
     * Creates a SectionTag for the parsed {@link SectionType} and an optional tag
     * (typically the XML element used to create this section)
     * @param section the type of the section (MUST NOT be <code>null</code>)
     * @param tag the tag or <code>null</code> if not known. In this case the
     * {@link SectionType#tag()} will be used as default.
     * @throws IllegalArgumentException if the parsed section is <code>null</code>
     * or empty.
     */
    @JsonCreator
    public SectionTag(SectionType section, String tag){
        super(tag == null ? section == null ? null : section.tag() : tag);
        if(section == null){
            throw new IllegalArgumentException("The parsed Section MUST NOT be NULL!");
        }
        this.section = section;
    }
    /**
     * The Section of this tag
     * @return the Section
     */
    public SectionType getSection(){
       return section; 
    }
    
    @Override
    public String toString() {
        return String.format("Section %s (tag: %s)", section, tag);
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SectionTag && super.equals(obj) &&
                section.equals(((SectionTag)obj).section);
    }
    
    
}
