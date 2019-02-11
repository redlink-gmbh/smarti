/*
 * Copyright 2019 DB Systel GmbH
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
 */
package io.redlink.smarti.query.google;

import io.redlink.smarti.model.Query;
import io.redlink.smarti.query.google.GoogleSearchConfiguration.ResultConfig;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://developers.google.com/custom-search/v1/cse/list
 */
public class GoogleSearchQuery extends Query {

    @JsonProperty("resultConfig")
    private final ResultConfig resultConfig;

    @JsonProperty("defaults")
    private final Map<String,Object> defaults;

    @JsonProperty("queryParams")
    private Set<String> queryParams = new LinkedHashSet<>();

    @JsonCreator
    public GoogleSearchQuery(@JsonProperty("creator") String creator,
            @JsonProperty("resultConfig") ResultConfig resultConfig,
            @JsonProperty("defaults") Map<String,Object> defaults) {

        super(creator);
        if(resultConfig == null){
            throw new NullPointerException("ResultConfig MUST NOT be NULL!");
        }
        this.resultConfig = resultConfig;
        this.defaults = defaults == null ? Collections.emptyMap() : Collections.unmodifiableMap(defaults);
    }

    public final void addQueryParam(String param){
        if(StringUtils.isNoneBlank(param)){
            this.queryParams.add(param);
        }
    }
    public final void setQueryParam(String param) {
        this.queryParams.clear();
        addQueryParam(param);
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

    @JsonGetter
    public Map<String, Object> getDefaults() {
        return defaults;
    }
    @JsonGetter
    public final ResultConfig getResultConfig() {
        return resultConfig;
    }

    public String toQueryString() {

        StringBuilder sb = new StringBuilder();
        sb.append("&");
        sb.append(MapQuery.urlEncodeUTF8(this.defaults));
        sb.append("&q=");
        sb.append(String.join(" ", queryParams));
        return sb.toString();
    }
    @Override
    public String toString() {
        return "GoogleSearchQuery [title=" + getDisplayTitle() + ", creator=" + getCreator() + ",params=" + queryParams + "]";
    }
}

