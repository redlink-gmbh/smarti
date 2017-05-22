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
 * Tracks {@link ValueTypeSerializer} implementations by using the {@link ServiceLoader}
 * utility
 * @author Rupert Westenthaler
 *
 */
@Component
public class ValueTypeSerializerRegistry {
    
    private final Logger log = LoggerFactory.getLogger(ValueTypeSerializerRegistry.class);

    private static ValueTypeSerializerRegistry instance;
    /**
     * Should be used when running outside of OSGI to create the singleton
     * instance of this factory.
     * @return the singleton instance
     */
    public static final ValueTypeSerializerRegistry getInstance(){
        if(instance == null){
            instance = new ValueTypeSerializerRegistry();
        }
        return instance;
    }
    
    ReadWriteLock serializerLock = new ReentrantReadWriteLock();

    Map<String,ValueTypeSerializer<?>> valueTypeSerializers;
    
    protected ValueTypeSerializerRegistry(){}
    
    @Autowired(required=false)
    public ValueTypeSerializerRegistry(Collection<ValueTypeSerializer<?>> serializers){
        if(serializers != null && !serializers.isEmpty()){
            valueTypeSerializers = new HashMap<>();
            for(ValueTypeSerializer<?> serializer : serializers){
                addSerializer(serializer);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> ValueTypeSerializer<T> getSerializer(String type){
        if(valueTypeSerializers == null){
            initValueTypeSerializer(); //running outside OSGI
        }
        serializerLock.readLock().lock();
        try {
            ValueTypeSerializer<T> serializer = (ValueTypeSerializer<T>)valueTypeSerializers.get(type);
            log.trace(" - lookup serializer for {} -> {}",type, serializer);
            return serializer;
        } finally {
            serializerLock.readLock().unlock();
        }
    }
    
    /**
     * Only used when running outside an OSGI environment
     */
    @SuppressWarnings("rawtypes")
    private void initValueTypeSerializer() {
        serializerLock.writeLock().lock();
        try {
            if(valueTypeSerializers == null){
                valueTypeSerializers = new HashMap<String,ValueTypeSerializer<?>>();
                ServiceLoader<ValueTypeSerializer> loader = ServiceLoader.load(ValueTypeSerializer.class);
                log.debug("load ValueTypeSerializers ...");
                for(Iterator<ValueTypeSerializer> it = loader.iterator();it.hasNext();){
                    addSerializer(it.next());
                }
            }
        } finally {
            serializerLock.writeLock().unlock();
        }
    }

    private void addSerializer(ValueTypeSerializer<?> vts) {
        ValueTypeSerializer<?> serializer = valueTypeSerializers.get(vts.getKey());
        if(serializer != null){
            log.warn("Multiple Serializers for key {} (keep: {}, ignoreing: {}",
                new Object[]{vts.getKey(),serializer,vts});
        } else {
                log.debug("   {} -> {}", vts.getKey(), vts.getClass().getName());
                valueTypeSerializers.put(vts.getKey(), vts);
        }
        String className = vts.getType().getName();
        if(className != null && !className.equals(vts.getKey())){
            serializer = valueTypeSerializers.get(className);
            if(serializer != null){
                log.warn("Multiple Serializers for type {} (keep: {}, ignoreing: {}",
                    new Object[]{className,serializer,vts});
            } else {
                log.debug("   {} -> {}", className, vts.getClass().getName());
                valueTypeSerializers.put(className, vts);
            }
        }
    }

}
