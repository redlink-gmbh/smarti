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
import io.redlink.nlp.model.Span;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.dep.GrammaticalRelation;
import io.redlink.nlp.model.dep.RelTag;
import io.redlink.nlp.model.dep.Relation;
import io.redlink.nlp.model.json.valuetype.ValueTypeParser;
import io.redlink.nlp.model.json.valuetype.ValueTypeSerializer;
import org.springframework.stereotype.Component;

@Component
public class DependencyRelationSupport implements ValueTypeParser<Relation>,
        ValueTypeSerializer<Relation> {

    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation";

    private static final String RELATION_TYPE_TAG = "tag";
    private static final String RELATION_STANBOL_TYPE_TAG = "relationType";
    private static final String RELATION_IS_DEPENDENT_TAG = "isDependent";
    private static final String RELATION_PARTNER_TYPE_TAG = "partnerType";
    private static final String RELATION_PARTNER_START_TAG = "partnerStart";
    private static final String RELATION_PARTNER_END_TAG = "partnerEnd";
    private static final String ROOT_TAG = "ROOT";

    @Override
    public ObjectNode serialize(ObjectMapper mapper, Relation relation) {
        ObjectNode jDependencyRelation = mapper.createObjectNode();

        RelTag gramRelTag = relation.getGrammaticalRelationTag();
        jDependencyRelation.put(RELATION_TYPE_TAG, gramRelTag.getTag());
        jDependencyRelation.put(RELATION_STANBOL_TYPE_TAG, gramRelTag.getRelation().ordinal());
        jDependencyRelation.put(RELATION_IS_DEPENDENT_TAG, (relation.isDependent()));

        Span partner = relation.getPartner();
        if (partner != null) {
            jDependencyRelation.put(RELATION_PARTNER_TYPE_TAG, partner.getType().toString());
            jDependencyRelation.put(RELATION_PARTNER_START_TAG, partner.getStart());
            jDependencyRelation.put(RELATION_PARTNER_END_TAG, partner.getEnd());
        } else {
            jDependencyRelation.put(RELATION_PARTNER_TYPE_TAG, ROOT_TAG);
            jDependencyRelation.put(RELATION_PARTNER_START_TAG, 0);
            jDependencyRelation.put(RELATION_PARTNER_END_TAG, 0);
        }

        return jDependencyRelation;
    }

    @Override
    public String getKey() {
        return TYPE_VALUE;
    }

    @Override
    public Class<Relation> getType() {
        return Relation.class;
    }
    
    @Override
    public Relation parse(ObjectNode jDependencyRelation, AnalyzedText at) {
        JsonNode tag = jDependencyRelation.path(RELATION_TYPE_TAG);

        if (!tag.isTextual()) {
            throw new IllegalStateException("Unable to parse GrammaticalRelationTag. The value of the "
                                            + "'tag' field MUST have a textual value (json: "
                                            + jDependencyRelation + ")");
        }

        GrammaticalRelation grammaticalRelation = GrammaticalRelation.class.getEnumConstants()[jDependencyRelation
                .path(RELATION_STANBOL_TYPE_TAG).asInt()];
        RelTag gramRelTag = new RelTag(tag.asText(), grammaticalRelation);

        JsonNode isDependent = jDependencyRelation.path(RELATION_IS_DEPENDENT_TAG);

        if (!isDependent.isBoolean()) {
            throw new IllegalStateException("Field 'isDependent' must have a true/false format");
        }

        Span partnerSpan = null;
        String typeString = jDependencyRelation.path(RELATION_PARTNER_TYPE_TAG).asText();

        if (!typeString.equals(ROOT_TAG)) {
            SpanTypeEnum spanType = SpanTypeEnum.valueOf(jDependencyRelation.path(RELATION_PARTNER_TYPE_TAG)
                    .asText());
            int spanStart = jDependencyRelation.path(RELATION_PARTNER_START_TAG).asInt();
            int spanEnd = jDependencyRelation.path(RELATION_PARTNER_END_TAG).asInt();

            switch (spanType) {
                case Chunk:
                    partnerSpan = at.addChunk(spanStart, spanEnd);
                    break;
                // unused types
                // case Sentence:
                // case Text:
                // case TextSection:
                // break;
                case Token:
                    partnerSpan = at.addToken(spanStart, spanEnd);
                    break;
                default: //nothing to do
                    break;
            }
        }

        return new Relation(gramRelTag, isDependent.asBoolean(), partnerSpan);
    }
}
