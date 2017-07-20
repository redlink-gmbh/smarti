package io.redlink.smarti.api.config;

import java.util.Map;
import java.util.Set;

import io.redlink.smarti.model.config.ComponentConfiguration;

public interface Configurable<CT extends ComponentConfiguration> {

    /**
     * The name of the category of this component (e.g. '<code>queryBuilder</code>')
     * @return the category name (typically related to the interface implemented by the component)
     */
    String getComponentCategory();
    /**
     * The name of the component. This is the key used within the category for
     * configurations of this type (e.g. the {@link Class#getName()} of the component)
     * @return the name of the component
     */
    String getComponentName();
    
    /**
     * The expected Java type of the configuration
     * @return the java type of the configuration
     */
    Class<CT> getComponentType();
    
    /**
     * The default configuration for this component
     * @return the default configuration
     */
    CT getDefaultConfiguration();
    
    /**
     * Validates a configuration for this component
     * @param configuration the configuration to validate
     * @param missing set to add field name/paths for missing required fields
     * @param conflicting map to add field names/paths of fields with conflicting values. The
     * value can be used for a error message why the parsed value is conflicting.
     * @return <code>true</code> if the configuration is valid or <code>false</code> if
     * the invalid. If <code>false</code> is returned the method is expected to add
     * entries to <code>missing</code> and/or <code>conflicting</code>
     */
    boolean validate(CT configuration, Set<String> missing, Map<String, String> conflicting);
    
    
}
