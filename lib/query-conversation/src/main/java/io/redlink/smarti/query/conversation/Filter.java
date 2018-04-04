package io.redlink.smarti.query.conversation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Filter for a query for related conversations.
 * <p>
 * {@link #isOptional() Optional} filters can be deactivated by the
 * client - meaning that they can be presented in the UI to be enabled/disabled
 * by the user. The {@link #isEnabled() enabled} state determines the default. For
 * required filters this is always <code>true</code>.
 * 
 * The {@link #getName() name} is typically a key pointing to the <code>i18n</code>
 * configuration. The {@link #getDisplayValue()} (if present) is the value of the
 * Filter to be shown in the UI. The {@link #getFilter()} is the value of the filter
 * to be included in the query the the backend.
 */
public class Filter {
    
    private String name;
    private boolean optional;
    private boolean enabled = true;
    private String filter;
    private String displayValue;
    
    @JsonCreator
    public Filter(@JsonProperty("name")String name, @JsonProperty("filter") String filter) {
        setName(name);
        setFilter(filter);
    }
    public String getName() {
        return name;
    }
    public Filter setName(String name) {
        this.name = name;
        return this;
    }
    public boolean isOptional() {
        return optional;
    }
    public Filter setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public Filter setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    public String getFilter() {
        return filter;
    }
    public Filter setFilter(String filter) {
        this.filter = filter;
        return this;
    }
    public String getDisplayValue() {
        return displayValue;
    }
    public Filter setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
        return this;
    }

    
    @Override
    public String toString() {
        return "Filter [name=" + name + ", optional=" + optional + ", filter=" + filter + "]";
    }
    
    
    
}