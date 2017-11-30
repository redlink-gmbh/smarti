/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResult<T> {

    private long numFound, start, pageSize;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Float maxScore;

    private List<T> docs;

    @JsonIgnore
    private final Map<String, Object> params = new HashMap<>();
    
    public SearchResult() {
        this(Collections.emptyList());
    }

    public SearchResult(long pageSize) {
        this(0, 0, pageSize, Collections.emptyList());
    }

    public SearchResult(List<T> docs) {
        this(docs.size(), 0, docs);
    }

    public SearchResult(long numFound, long start, List<T> docs) {
        this(numFound, start, docs.size(), docs);
    }

    public SearchResult(long numFound, long start, long pageSize, List<T> docs) {
        this.numFound = numFound;
        this.start = start;
        this.pageSize = pageSize;
        this.docs = docs;
    }

    public long getNumFound() {
        return numFound;
    }

    public SearchResult<T> setNumFound(long numFound) {
        this.numFound = numFound;
        return this;
    }

    public long getStart() {
        return start;
    }

    public SearchResult<T> setStart(long start) {
        this.start = start;
        return this;
    }

    public long getPageSize() {
        return pageSize;
    }

    public SearchResult<T> setPageSize(long pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public SearchResult<T> setMaxScore(float maxScore) {
        this.maxScore = maxScore;
        return this;
    }

    public List<T> getDocs() {
        return docs;
    }

    public SearchResult<T> setDocs(List<T> docs) {
        this.docs = docs;
        return this;
    }
    
    @JsonAnyGetter
    public Map<String, Object> getParams() {
        return params;
    }
    
    @JsonAnySetter
    protected void setParam(String name, Object value) {
        params.put(name, value);
    }
    
    
}
