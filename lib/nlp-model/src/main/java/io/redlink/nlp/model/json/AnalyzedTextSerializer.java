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
package io.redlink.nlp.model.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Span;
import io.redlink.nlp.model.Span.SpanTypeEnum;
import io.redlink.nlp.model.Value;
import io.redlink.nlp.model.json.valuetype.ValueTypeSerializer;
import io.redlink.nlp.model.json.valuetype.ValueTypeSerializerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Serializes an AnalyzedText instance as JSON
 * @author Rupert Westenthaler
 *
 */
@Component
public class AnalyzedTextSerializer {
    
    private final Logger log = LoggerFactory.getLogger(AnalyzedTextSerializer.class);

    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    private static AnalyzedTextSerializer defaultInstance;
    protected ObjectMapper mapper = new ObjectMapper();  
    
    protected final ValueTypeSerializerRegistry valueTypeSerializerRegistry;

    /**
     * Can be used when running outside of OSGI to obtain the default (singelton)
     * instance.
     * @return
     */
    public static final AnalyzedTextSerializer getDefaultInstance(){
        if(defaultInstance == null){
            defaultInstance = new AnalyzedTextSerializer(ValueTypeSerializerRegistry.getInstance());
        }
        return defaultInstance;
    }
    
    /**
     * Default constructor used by OSGI
     */
    public AnalyzedTextSerializer() {
        this(null);
    }
    
    /**
     * Constructs a new Serializer instance for the parsed {@link ValueTypeSerializerRegistry}
     * instance. Typically this constructor should not be used as usages within
     * an OSGI environment MUST lookup the service via the service registry.
     * Usages outside an OSGI environment should prefer to use the
     * {@link #getDefaultInstance()} instance to obtain the singleton instance.
     * @param vtsr
     */
    @Autowired(required=false)
    public AnalyzedTextSerializer(ValueTypeSerializerRegistry vtsr){
        if(vtsr == null){
            vtsr = ValueTypeSerializerRegistry.getInstance();
        }
        this.valueTypeSerializerRegistry = vtsr;
    }
    
    /**
     * Serializes the parsed {@link AnalyzedText} to the {@link OutputStream} by
     * using the {@link Charset}.
     * @param at the {@link AnalyzedText} to serialize
     * @param out the {@link OutputStream} 
     * @param charset the {@link Charset}. UTF-8 is used as default if <code>null</code>
     * is parsed
     */
    public void serialize(AnalyzedText at, OutputStream out, Charset charset) throws IOException {
        if(at == null){
            throw new IllegalArgumentException("The parsed AnalyzedText MUST NOT be NULL!");
        }
        if(out == null){
            throw new IllegalArgumentException("The parsed OutputStream MUST NOT be NULL");
        }
        if(charset == null){
            charset = UTF8;
        }
        JsonFactory jsonFactory = mapper.getFactory();
        JsonGenerator jg = jsonFactory.createGenerator(new OutputStreamWriter(out, charset));
        serialize(at, jg);
    }
    /**
     * Serializes the parsed {@link AnalyzedText} to a JSON formatted String
     * @param at the {@link AnalyzedText} to serialize
     * @return the JSON serialized String
     * @throws IOException
     */
    public String toJsonString(AnalyzedText at) throws IOException {
        StringWriter sw = new StringWriter();
        serialize(at, mapper.getFactory().createGenerator(sw));
        return sw.toString();
    }
    
    private void serialize(AnalyzedText at, JsonGenerator jg) throws IOException {
        jg.useDefaultPrettyPrinter();
        jg.writeStartObject();
        jg.writeArrayFieldStart("spans");
        jg.writeTree(writeSpan(at));
        for(Iterator<Span> it = at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));it.hasNext();){
            jg.writeTree(writeSpan(it.next()));
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.close();
    }

    private ObjectNode writeSpan(Span span) throws IOException {
        log.trace("wirte {}",span);
        ObjectNode jSpan = mapper.createObjectNode();
        jSpan.put("type", span.getType().name());
        jSpan.put("start", span.getStart());
        jSpan.put("end", span.getEnd());
        for(String key : span.getKeys()){
            List<Value<?>> values = span.getValues(key);
            //in MongoDB we use '_' instead of '.' so if we convert back to
            //Stanbol compatible JSON we need to replace those with '.'
            key = key.replace('_', '.'); 
            if(values.size() == 1){
                jSpan.set(key, writeValue(values.get(0)));
            } else {
                ArrayNode jValues = jSpan.putArray(key);
                for(Value<?> value : values){
                    jValues.add(writeValue(value));
                }
                jSpan.set(key, jValues);
            }
        }
        log.trace(" ... {}",jSpan);
        return jSpan;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ObjectNode writeValue(Value<?> value) {
        ObjectNode jValue;
        Class<?> valueType = value.value().getClass();
        ValueTypeSerializer vts = valueTypeSerializerRegistry.getSerializer(valueType.getName());
        if(vts != null){
            jValue = vts.serialize(mapper,value.value());
            jValue.put("class",vts.getKey()); //use the key as class!
            //TODO assert that jValue does not define "class" and "prob"!
        } else { //use the default binding and the "data" field
            jValue = mapper.createObjectNode();
            jValue.set("value", mapper.valueToTree(value.value()));
            jValue.put("class",valueType.getName());
        }
        if(value.probability() != Value.UNKNOWN_PROBABILITY){
            jValue.put("prob", value.probability());
        }
        return jValue;
    }    
}
