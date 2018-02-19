package io.redlink.smarti.processing;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.redlink.smarti.api.config.Configurable;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.PrepareService;

/**
 * Allows to configure the analysis language for a client
 * @author Rupert Westenthaler
 *
 */
@Component
public class AnalysisLanguageConfiguration implements Configurable<ComponentConfiguration> {

    public static String KEY_LANGUAGE = "language";
    
    @Override
    public String getComponentCategory() {
        return PrepareService.ANALYSIS_CONFIGURATION_CATEGORY;
    }

    @Override
    public String getComponentName() {
        return "language";
    }

    @Override
    public Class<ComponentConfiguration> getConfigurationType() {
        return ComponentConfiguration.class;
    }

    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        ComponentConfiguration cc = new ComponentConfiguration();
        cc.setConfiguration(KEY_LANGUAGE, "");
        cc.setDisplayName("Language");
        cc.setEnabled(false);
        return cc;
    }

    @Override
    public boolean validate(ComponentConfiguration configuration, Set<String> missing,
            Map<String, String> conflicting) {
        return configuration.isConfiguration(KEY_LANGUAGE);
    }
    
}