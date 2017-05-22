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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tracks {@link ValueTypeParser} implementations by using the {@link ServiceLoader}
 * utility 
 * @author Rupert Westenthaler
 *
 */
@Component
public class ValueTypeParserRegistry {
    private final Logger log = LoggerFactory.getLogger(ValueTypeParserRegistry.class);

    private static ValueTypeParserRegistry instance;
    /**
     * Should be used when running outside of OSGI to create the singleton
     * instance of this factory.
     * @return the singleton instance
     */
    public static final ValueTypeParserRegistry getInstance(){
        if(instance == null){
            instance = new ValueTypeParserRegistry();
        }
        return instance;
    }

    ReadWriteLock parserLock = new ReentrantReadWriteLock();
    /**
     * Used outside OSGI
     */
    Map<String,ValueTypeParser<?>> valueTypeParsers;

    
    protected ValueTypeParserRegistry(){}
    
    @Autowired(required=false)
    public ValueTypeParserRegistry(Collection<ValueTypeParser<?>> parsers){
        if(parsers != null && parsers.isEmpty()){
            valueTypeParsers = new HashMap<>();
            for(ValueTypeParser<?> parser : parsers){
                addParser(parser);
            }
        } //else call #initValueTypeParsers() on first usage
    }
    
    @SuppressWarnings("unchecked")
    public <T> ValueTypeParser<T> getParser(String type){
        if(valueTypeParsers == null){
            initValueTypeParser(); //running outside a framework
        }
        parserLock.readLock().lock();
        try {
            if(log.isTraceEnabled()){
                log.trace(" - lookup parser for {} -> {}",type, valueTypeParsers.get(type));
            }
            return (ValueTypeParser<T>)valueTypeParsers.get(type);
        } finally {
            parserLock.readLock().unlock();
        }
    }
    
    /**
     * When running outside a framework with injection we use {@link ServiceLoader}
     * to look for services
     */
    @SuppressWarnings("rawtypes")
    private void initValueTypeParser() {
        parserLock.writeLock().lock();
        try {
            if(valueTypeParsers == null){
                valueTypeParsers = new HashMap<>();
                ServiceLoader<ValueTypeParser> loader = ServiceLoader.load(ValueTypeParser.class);
                log.debug("load ValueTypeParsers ...");
                for(Iterator<ValueTypeParser> it = loader.iterator();it.hasNext();){
                    try {
                        ValueTypeParser vtp = it.next();
                        //register serializer for both the key and the class name of the type
                        addParser(vtp);
                    } catch (NoClassDefFoundError e) {
                        //ignore services that can not be loaded
                        //e.g. when mixing different version of the stanbol.enhancer.nlp
                        //and the stanbol.enhancer.nlp-json module
                        //It is better to throw an exception if an Node for the failed
                        //ValueTypeParser appears in the JSON as when loading all
                        //registered services
                        log.warn("Unable to load a ValueTypeParser service because class '"
                            +e.getMessage()+" could not be loaded! This may happen if the "
                            + "classpath mixes different versions of o.a.stanbol.enhancer.nlp* "
                            + "modules!");
                    }
                }
            }
        } finally {
            parserLock.writeLock().unlock();
        }
    }

    private void addParser(ValueTypeParser<?> vtp) {
        ValueTypeParser<?> parser = valueTypeParsers.get(vtp.getKey());
        if(parser != null){
            log.warn("Multiple Parsers for key {} (keep: {}, ignoreing: {}",
                new Object[]{vtp.getKey(),parser,vtp});
        } else {
            log.debug("   {} -> {}", vtp.getKey(), vtp.getClass().getName());
            valueTypeParsers.put(vtp.getKey(), vtp);
        }
        String className = vtp.getType().getName();
        if(className != null && !className.equals(vtp.getKey())){
            parser = valueTypeParsers.get(className);
            if(parser != null){
                log.warn("Multiple Parsers for type {} (keep: {}, ignoreing: {}",
                    new Object[]{className,parser,vtp});
            } else {
                valueTypeParsers.put(className, vtp);
            }
        }
    }
}
