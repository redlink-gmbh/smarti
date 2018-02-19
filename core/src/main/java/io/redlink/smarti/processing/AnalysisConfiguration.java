package io.redlink.smarti.processing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="smarti.analysis")
public class AnalysisConfiguration {

    public static final String DEFAULT_LANGUAGE = null; //no default
    
    private String language = DEFAULT_LANGUAGE;
    private Pipeline pipeline = new Pipeline();
    
    public final String getLanguage() {
        return language;
    }
    
    public final void setLanguage(String language) {
        this.language = language;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }
    
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    public static class Pipeline {
        private String required;
        
        private String optional;

        
        public String getRequired() {
            return required;
        }
        
        public void setRequired(String required) {
            this.required = required;
        }
        
        
        public String getOptional() {
            return optional;
        }
        
        public void setOptional(String optional) {
            this.optional = optional;
        }
        
    }
}
