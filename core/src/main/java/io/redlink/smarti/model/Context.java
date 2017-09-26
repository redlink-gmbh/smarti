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

import java.util.HashMap;
import java.util.Map;

/**
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    /**
     * {@link #getEnvironment(String) Environment key} to store the name of the channel
     * the conversation was part of
     */
    public static final String ENV_CHANNEL_NAME = "channel";
    /**
     * {@link #getEnvironment(String) Environment key} to store the id of the channel
     * the conversation was part of
     */
    public static final String ENV_CHANNEL_ID = "channel_id";
    /**
     * {@link #getEnvironment(String) Environment key} to store the user token of the
     * conversation
     */
    public static final String ENV_TOKEN = "token";
    /**
     * the Channel-Topic
     */
    public static final String ENV_SUPPORT_AREA = "expertise";

    private String contextType; // = "ApplicationHelp"
    private String environmentType; // = "SAP_Application"
    private String domain;
    private Map<String,String> environment = new HashMap<>();

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

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public String getEnvironment(String key) {
        return environment.get(key);
    }

    public String getEnvironment(String key, String defaultValue) {
        return environment.getOrDefault(key, defaultValue);
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public void setEnvironment(String key, String value) {
        environment.put(key, value);
    }
}
