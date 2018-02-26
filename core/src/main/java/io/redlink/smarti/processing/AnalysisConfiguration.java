package io.redlink.smarti.processing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="smarti.analysis")
public class AnalysisConfiguration {

    public static final String DEFAULT_LANGUAGE = null; //no default
    
    public static final int DEFAULT_CONTEXT_SIZE = 10;
    public static final int MIN_CONTEXT_SIZE = 3;
    
    private String language = DEFAULT_LANGUAGE;
    private int conextSize = DEFAULT_CONTEXT_SIZE;
    
    private Pipeline pipeline = new Pipeline();
    
    public final String getLanguage() {
        return language;
    }
    
    public final void setLanguage(String language) {
        this.language = language;
    }
    /**
     * The context size (<code>-1</code> if none)
     * @return the context size or <code>-1</code> if none)
     */
    public int getConextSize() {
        return conextSize < 0 ? -1 : conextSize < MIN_CONTEXT_SIZE ? MIN_CONTEXT_SIZE : conextSize;
    }
    
    public void setConextSize(int conextSize) {
        this.conextSize = conextSize;
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
