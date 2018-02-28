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

package io.redlink.smarti.query.conversation;


import io.redlink.smarti.model.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
public class ConversationSearchQuery extends Query {

    @JsonProperty("defaults")
    private final Map<String,Object> defaults = new HashMap<>();
    @JsonProperty("keywords")
    private final Collection<String> keywords = new LinkedHashSet<>();
    @JsonProperty("terms")
    private final Collection<String> terms = new LinkedHashSet<>();
    @JsonProperty("queryParams")
    private final Set<String> queryParams = new LinkedHashSet<>();
    @JsonProperty("filterQueries")
    private final Set<String> filterQueries = new LinkedHashSet<>();

    public ConversationSearchQuery(@JsonProperty("creator") String creator) {
        super(creator);
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }
    
    public void setDefaults(Map<String, Object> defaults) {
        this.defaults.clear();
        if(defaults != null){
            this.defaults.putAll(defaults);
        }
    }
    
    public void addFilterQuery(String filter){
        filterQueries.add(filter);
    }
    
    public Collection<String> getFilterQueries() {
        return filterQueries;
    }
    
    public void setFilters(Collection<String> filtersQueries) {
        this.filterQueries.clear();
        if(filtersQueries != null){
            this.filterQueries.addAll(filtersQueries);
        }
    }

    public void setKeywords(Collection<String> strs) {
        keywords.clear();
        if(strs != null){
            keywords.addAll(strs);
        }
    }
    
    public Collection<String> getKeywords() {
        return keywords;
    }

    public void setTerms(Collection<String> strs) {
        terms.clear();
        if(strs != null){
            terms.addAll(strs);
        }
    }
    
    public Collection<String> getTerms() {
        return terms;
    }

}
