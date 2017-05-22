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
package io.redlink.nlp.model;

import java.util.*;

public class Annotated{

    private Map<String,Object> annotations;
    
    /**
     * Getter for all keys used by Annotations
     * @return the Set with all keys. An empty Set if none
     */
    @SuppressWarnings("unchecked")
    public Set<String> getKeys(){
        return annotations == null ? Collections.EMPTY_SET : annotations.keySet();
    }
    
    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return the Value with the highest probability
     */
    public final Value<?> getValue(String key) {
        if(annotations == null){
            return null;
        }
        Object value = annotations.get(key);
        if(value instanceof Value<?>){
            return (Value<?>)value;
        } else if(value != null){
            return ((List<Value<?>>)value).get(0);
        } else {
            return null;
        }
    }
    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return all Value sorted by probability
     */
    @SuppressWarnings("unchecked")
    public final List<Value<?>> getValues(String key) {
        if(annotations == null){
            return Collections.emptyList();
        }
        Object value = annotations.get(key);
        if(value instanceof Value<?>){
            List<?> singleton = Collections.singletonList((Value<?>)value);
            return (List<Value<?>>)singleton;
        } else if (value != null){
            return Collections.unmodifiableList((List<Value<?>>)value);
        } else {
            return Collections.emptyList();
        }
    }
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return the Value with the highest probability
     * @throws ClassCastException if values of {@link Annotation#getKey()} are
     * not of type V
     */
    @SuppressWarnings("unchecked")
    public final <V> Value<V> getAnnotation(Annotation<V> annotation) {
        if(annotations == null){
            return null;
        }
        Object value = annotations.get(annotation.getKey());
        if(value instanceof Value<?>){
            return (Value<V>)value;
        } else if(value != null){
            return ((List<Value<V>>)value).get(0);
        } else {
            return null;
        }
    }
    
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return all Values sorted by probability
     * @throws ClassCastException if the returned value of 
     * {@link Annotation#getKey()} is not of type V
     */
    @SuppressWarnings("unchecked")
    public final <V> List<Value<V>> getAnnotations(Annotation<V> annotation) {
        if(annotations == null){
            return Collections.emptyList();
        }
        Object value = annotations.get(annotation.getKey());
        if(value instanceof Value<?>){
            List<?> singleton = Collections.singletonList((Value<?>)value);
            return (List<Value<V>>)singleton;
        } else if(value != null){
            return Collections.unmodifiableList((List<Value<V>>)value);
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param values the values to append
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <V> void addAnnotations(Annotation<V> annotation, List<Value<V>> values) {
        addValuesInternal(annotation.getKey(), (List)values);
    }
    /**
     * Appends the parsed values to the key. This method is intended for internal use (
     * e.g. parsers). Users are encouraged to define type save
     * {@link Annotation} objects and use {@link #addAnnotations(Annotation, List)}
     * instead. 
     * @param key the key
     * @param values the values
     */
    public void addValues(String key, List<Value<?>> values) {
        addValuesInternal(key, values);
    }
    /**
     * Just here because of Java generics combined with Collections ...
     * @param key the key
     * @param values the values
     */
    @SuppressWarnings("unchecked")
    private void addValuesInternal(String key, List<Value<?>> values) {
        if(values == null || values.isEmpty()){
            return;
        }
        Map<String, Object> map = initAnnotations();
        Object currentValue = annotations.get(key);
        Object newValues;
        if(currentValue == null){
            if(values.size() == 1){
                newValues = values.get(0);
            } else {
                values = new ArrayList<>(values); //copy
                Collections.sort(values, Value.PROBABILITY_COMPARATOR); //sort
                newValues = values;
            }
        } else if (currentValue instanceof Value<?>){
            List<Value<?>> newValuesList = new ArrayList<>(Math.max(4,values.size()+1));
            newValuesList.add((Value<?>)currentValue);
            newValuesList.addAll(values);
            Collections.sort(newValuesList, Value.PROBABILITY_COMPARATOR); //sort
            newValues = newValuesList;
        } else { //an ArrayList
            ((List<Value<?>>) currentValue).addAll(values);
            Collections.sort((List<Value<?>>) currentValue, Value.PROBABILITY_COMPARATOR); //sort
            newValues = null; //no need to put new values
        }
        if(newValues != null){
            map.put(key, newValues);
        }
    }
    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param values the values for the annotation
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <V> void setAnnotations(Annotation<V> annotation, List<Value<V>> values) {
        setValuesInternal(annotation.getKey(), (List)values);
    }
    /**
     * Replaces existing Values for a key with the parsed one. This method is 
     * intended for internal use (e.g. parsers). Users are encouraged to define 
     * type save {@link Annotation} objects and use 
     * {@link #setAnnotations(Annotation, List)} instead. 
     * @param key the key
     * @param values the values
     */
    public void setValues(String key, List<Value<?>> values){
        setValuesInternal(key, values);
    }
    /**
     * Just here because of Java generics combined with Collections ...
     * @param key the key
     * @param values the values
     */
    private void setValuesInternal(String key, List<Value<?>> values){
        Map<String, Object> map = initAnnotations();
        if(values == null || values.isEmpty()){
            map.remove(key);
        } else if(values.size() == 1){
            map.put(key, values.get(0));
        } else {
            //we need to copy, because users might change the parsed Array!
            values = new ArrayList<>(values);
            Collections.sort(values,Value.PROBABILITY_COMPARATOR);
            map.put(key,values);
        }
        
    }
    
    private Map<String,Object> initAnnotations(){
        if(annotations == null){ //avoid sync for the typical case
            annotations = new HashMap<String,Object>();
        }
        return annotations;
    }
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param value the value to append
     */
    public <V> void addAnnotation(Annotation<V> annotation, Value<V> value) {
        addValue(annotation.getKey(), value);
    }
    /**
     * Appends an Value to the key. This method is intended for internal use (
     * e.g. parsers). Users are encouraged to define type save
     * {@link Annotation} objects and use {@link #addAnnotation(Annotation, Value)}
     * instead. 
     * @param key the key
     * @param value the value
     */
    public void addValue(String key, Value<?> value) {
        if(value != null){
          Map<String,Object> map = initAnnotations();  
          Object currentValue = map.get(key);
          if(currentValue == null){
              map.put(key, value);
          } else if (currentValue instanceof Value<?>){
              List<Value<?>> newValues = new ArrayList<>(4);
              newValues.add((Value<?>)currentValue);
              newValues.add(value);
              Collections.sort(newValues,Value.PROBABILITY_COMPARATOR);
              map.put(key, newValues);
          } else { //list
              List<Value<?>> currentValueList = (List<Value<?>>)currentValue;
              //insert the new value at the correct position
              int pos = Collections.binarySearch(currentValueList, value, Value.PROBABILITY_COMPARATOR);
              currentValueList.add(pos >= 0 ? pos : Math.abs(pos+1), value);
              //no put required
          }
        } 
    }
    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param value the value for the annotation
     */
    public <V> void setAnnotation(Annotation<V> annotation, Value<V> value) {
        setValue(annotation.getKey(), value);
    }
    /**
     * Replaces existing Values for a key with the parsed one. This method is 
     * intended for internal use (e.g. parsers). Users are encouraged to define 
     * type save {@link Annotation} objects and use 
     * {@link #setAnnotation(Annotation, Value)} instead. 
     * @param key the key
     * @param value the value
     */
    public void setValue(String key, Value<?> value) {
        if(annotations == null && value == null){
            return;
        }
        Map<String,Object> map = initAnnotations();
        if(value == null){
            map.remove(key);
        } else {
            map.put(key,value);
        }
    }
}
