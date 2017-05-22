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
package io.redlink.nlp.model.json.valuetype.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.json.JsonUtils;
import io.redlink.nlp.model.json.valuetype.ValueTypeParser;
import io.redlink.nlp.model.json.valuetype.ValueTypeSerializer;
import io.redlink.nlp.model.section.SectionTag;
import io.redlink.nlp.model.section.SectionType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SectionTagSupport implements ValueTypeParser<SectionTag>, ValueTypeSerializer<SectionTag> {

    public static final String TYPE_VALUE = SectionTag.class.getName();
    
    @Override
    public String getKey() {
        return TYPE_VALUE;
    }
    
    @Override
    public Class<SectionTag> getType() {
        return SectionTag.class;
    }
    
    @Override
    public SectionTag parse(ObjectNode jValue, AnalyzedText at) {
        JsonNode tag = jValue.path("tag");
        if(!tag.isTextual()){
            throw new IllegalStateException("Unable to parse SectionTag. The value of the "
                    +"'tag' field MUST have a textual value (json: "+jValue+")");
        }
        final SectionType section;
        if(jValue.has("section")){
            Set<SectionType> sections = JsonUtils.parseEnum(jValue, "section", SectionType.class);
            section = sections.isEmpty() ? null : sections.iterator().next();
        } else {
            throw new IllegalStateException("Unable to parse SectionTag. The required "
                    +"'section' field is missing!");
        }
        return new SectionTag(section, tag.asText());
    }



    @Override
    public ObjectNode serialize(ObjectMapper mapper, SectionTag value){
        ObjectNode jPosTag = mapper.createObjectNode();
        jPosTag.put("tag", value.getTag());
        if(value.getSection() != null){
            jPosTag.put("section",value.getSection().ordinal());
        }
        return jPosTag;
    }
    
}
