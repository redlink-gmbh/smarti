/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

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
