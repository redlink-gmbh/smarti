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

import java.util.*;

/**
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    private String contextType; // = "ApplicationHelp"
    private String environmentType; // = "SAP_Application"
    private String domain;
    private Map<String,List<String>> environment = new HashMap<>();

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getDomain() {
        return domain;
    }

    public Context setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Map<String, List<String>> getEnvironment() {
        return environment;
    }

    public List<String> getEnvironment(String key) {
        return environment.get(key);
    }

    public void setEnvironment(Map<String, List<String>> environment) {
        this.environment = environment;
    }

    public void setEnvironment(String key, String value) {
        setEnvironment(key, new LinkedList<>(Collections.singletonList(value)));
    }

    public void setEnvironment(String key, List<String> value) {
        environment.put(key, value);
    }
}
