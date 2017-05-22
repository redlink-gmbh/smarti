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
package io.redlink.nlp.model.json.valuetype;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Value;

import java.util.ServiceLoader;

/**
 * Interface allowing to extend how Classes used as generic type for
 * {@link Value}s are parsed from JSON
 * <p>
 * Implementations are loaded via Java {@link ServiceLoader}. So make sure 
 * the according <code>META-INF/services/io.redlink.nlp.model.json.valuetype.ValueTypeParser</code> 
 * meta data are available
 * <p>
 * Parsers define both {@link #getKey()} and {@link #getType()}. This ensures
 * compatibility for parsing AnalyzedText JSON generated components using the
 * Apache Stanbol NLP libraries.
 * 
 * @param <T>
 */
public interface ValueTypeParser<T> {


    String PROPERTY_TYPE = "type";

    /**
     * The <code>class</code> key supported by this value type parser
     * @return the keys supported by this parser
     */
    String getKey();
    
    /**
     * The type of objects created by this parser.
     * @return
     */
    Class<T> getType();
    
    T parse(ObjectNode jAnnotation, AnalyzedText at);
    
}
