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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class SearchResult<T> {

    private long numFound, start;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Float maxScore;

    private List<T> docs;

    public SearchResult() {
    }

    public SearchResult(long numFound, long start, List<T> docs) {
        this.numFound = numFound;
        this.start = start;
        this.docs = docs;
    }

    public long getNumFound() {
        return numFound;
    }

    public SearchResult setNumFound(long numFound) {
        this.numFound = numFound;
        return this;
    }

    public long getStart() {
        return start;
    }

    public SearchResult setStart(long start) {
        this.start = start;
        return this;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public SearchResult setMaxScore(float maxScore) {
        this.maxScore = maxScore;
        return this;
    }

    public List<T> getDocs() {
        return docs;
    }

    public SearchResult setDocs(List<T> docs) {
        this.docs = docs;
        return this;
    }
}
