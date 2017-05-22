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
package io.redlink.nlp.model.tag;

public abstract class Tag<T extends Tag<T>> { //lol ??!! is that how to define T

    
    protected final String tag;

    /**
     * Creates a PosTag for the given String
     * @param tag the tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public Tag(String tag){
        if(tag == null || tag.isEmpty()){
            throw new IllegalArgumentException("The tag MUST NOT be NULL!");
        }
        this.tag = tag;
    }
    
    public final String getTag() {
        return tag;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s ", getClass().getSimpleName(), tag);
    }

    @Override
    public int hashCode() {
        return 31 + tag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        if (!tag.equals(other.tag))
            return false;
        return true;
    }
    

}