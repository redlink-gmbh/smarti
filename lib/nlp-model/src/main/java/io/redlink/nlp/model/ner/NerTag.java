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
package io.redlink.nlp.model.ner;

import io.redlink.nlp.model.tag.Tag;
import org.springframework.data.annotation.PersistenceConstructor;

public class NerTag extends Tag<NerTag> {

    /**
     * Named Entity type string for Persons
     */
    public static final String NAMED_ENTITY_PERSON = "pers";
    /**
     * Named Entity type string for Organizations
     */
    public static final String NAMED_ENTITY_ORGANIZATION = "org";
    /**
     * Named Entity type string for Locations, Places ...
     */
    public static final String NAMED_ENTITY_LOCATION = "loc";
    /**
     * Named Entity type string for Events
     */
    public static final String NAMED_ENTITY_EVENT = "evt";
    /**
     * Named Entity type string for miscellaneous types (as often used by
     * NER classifier)
     */
    public static final String NAMED_ENTITY_MISC = "misc";
    /**
     * Named Entity type string for entities of unknown type. This type
     * is intended to be used for Named Entities that do not have any
     * type (e.g. if the NER engine does not provide type information).
     */
    public static final String NAMED_ENTITY_UNKOWN = "unk";

    private String type;
    
    public NerTag(String tag) {
        super(tag);
    }
    
    @PersistenceConstructor
    public NerTag(String tag,String type) {
        super(tag);
        this.type = type;
    }

    /**
     * The <code>dc:type</code> of the Named Entity
     * @return the <code>dc:type</code> of the Named Entity
     * as also used by the <code>fise:TextAnnotation</code>
     */
    public String getType() {
        return type == null ? NAMED_ENTITY_UNKOWN : type;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + getType().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NerTag other = (NerTag) obj;
        if (!getType().equals(other.getType()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NerTag [type=" + getType() + ", tag=" + tag  + "]";
    }
    
}
