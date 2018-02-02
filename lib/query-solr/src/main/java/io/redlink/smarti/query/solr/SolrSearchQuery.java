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

package io.redlink.smarti.query.solr;


import io.redlink.smarti.model.Query;
import io.redlink.smarti.query.solr.SolrEndpointConfiguration.ResultConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
public class SolrSearchQuery extends Query {

    @JsonProperty("resultConfig")
    private final ResultConfig resultConfig;
    @JsonProperty("defaults")
    private final Map<String,Object> defaults;
    @JsonProperty("queryParams")
    private Set<String> queryParams = new LinkedHashSet<>();
    @JsonProperty("filterQueries")
    private Set<String> filterQueries = new LinkedHashSet<>();

    @JsonCreator
    public SolrSearchQuery(@JsonProperty("creator") String creator, 
            @JsonProperty("resultConfig") ResultConfig resultConfig, 
            @JsonProperty("defaults") Map<String,Object> defaults) {
        super(creator);
        if(resultConfig == null){
            throw new NullPointerException("ResultConfig MUST NOT be NULL!");
        }
        this.resultConfig = resultConfig;
        this.defaults = defaults == null ? Collections.emptyMap() : Collections.unmodifiableMap(defaults);
    }

    public final void setQueryParam(String param) {
        this.queryParams.clear();
        addQueryParam(param);
    }
    public final void addQueryParam(String param){
        if(StringUtils.isNoneBlank(param)){
            this.queryParams.add(param);
        }
    }

    public final Collection<String> getQueryParams() {
        return queryParams;
    }

    public final void setQueryParams(Collection<String> params) {
        queryParams.clear();
        if(params != null){
            params.forEach(this::addQueryParam);
        }
    }
    
    public final void setFilterQuery(String fq) {
        this.filterQueries.clear();
        addFilterQuery(fq);
    }

    public final void addFilterQuery(String fq){
        if(StringUtils.isNoneBlank(fq)){
            this.filterQueries.add(fq);
        }
    }

    public final Collection<String> getFilterQueries() {
        return filterQueries;
    }

    public final void setFilterQueries(Collection<String> fqs) {
        filterQueries.clear();
        if(fqs != null){
            fqs.forEach(this::addFilterQuery);
        }
    }

    @JsonGetter
    public final ResultConfig getResultConfig() {
        return resultConfig;
    }
    
    /**
     * Additional Solr Parameters that SHOULD be parsed with the query as defaults
     * @return the solr default parameters
     */
    @JsonGetter
    public Map<String, Object> getDefaults() {
        return defaults;
    }

    @Override
    public String toString() {
        return "SolrSearchQuery [title=" + getDisplayTitle() + ", creator=" + getCreator() + ",params=" + queryParams + "]";
    }
}

