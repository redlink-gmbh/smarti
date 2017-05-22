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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.json.JsonUtils;
import io.redlink.nlp.model.json.valuetype.ValueTypeParser;
import io.redlink.nlp.model.json.valuetype.ValueTypeSerializer;
import io.redlink.nlp.model.phrase.PhraseCategory;
import io.redlink.nlp.model.phrase.PhraseTag;
import io.redlink.nlp.model.pos.LexicalCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class PhraseTagSupport implements ValueTypeParser<PhraseTag>, ValueTypeSerializer<PhraseTag> {

    private final Logger log = LoggerFactory.getLogger(PhraseTagSupport.class);
    
    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.ner.PhraseTag";

    
    @Override
    public String getKey() {
        return TYPE_VALUE;
    }

    @Override
    public Class<PhraseTag> getType() {
        return PhraseTag.class;
    }
    
    @Override
    public ObjectNode serialize(ObjectMapper mapper, PhraseTag value) {
        ObjectNode jTag = mapper.createObjectNode();
        jTag.put("tag",value.getTag());
        ArrayNode jPc = mapper.createArrayNode();
        LexicalCategory lc = null;
        for(PhraseCategory pc : value.getCategories()){
            jPc.add(pc.ordinal());
            if(lc == null){
                lc = pc.category();
            }
        }
        jTag.set("pc", jPc);
        if(lc != null){ //backward compatibility for old PhraseTag
            jTag.put("lc", lc.ordinal());
        }
        return jTag;
    }


    @Override
    public PhraseTag parse(ObjectNode jValue, AnalyzedText at) {
        JsonNode tag = jValue.path("tag");
        if(!tag.isTextual()){
            throw new IllegalStateException("Unable to parse PhraseTag. The value of the "
                    +"'tag' field MUST have a textual value (json: "+jValue+")");
        }
        EnumSet<PhraseCategory> pcs = JsonUtils.parseEnum(jValue,"pc",PhraseCategory.class);
        if(pcs.isEmpty()){ //try to guess PhraseCat based on "lc" value - backward compatibility
            JsonNode jLexCat = jValue.path("lc");
            LexicalCategory lc = null;
            if(jLexCat.isTextual()){
                try {
                    lc = LexicalCategory.valueOf(jLexCat.asText());
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to parse category for PhraseTag from '" 
                            +jLexCat.asText()+"' (will create with tag only)!",e);
                }
            } else if(jLexCat.isInt()){
                lc = LexicalCategory.values()[jLexCat.asInt()];
            } else if(!jLexCat.isMissingNode()){
                log.warn("Unable to parse category for PhraseTag from "+jLexCat
                    +"(will create with tag only)");
            }
            if(lc != null){
                PhraseCategory pc = PhraseCategory.getPhraseCategory(lc);
                if(pc != null){
                    pcs.add(pc);
                }
            }
        }
        return new PhraseTag(tag.asText(),pcs);
    }

}
