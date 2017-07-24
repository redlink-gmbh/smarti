package io.redlink.smarti.model.config;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import io.redlink.smarti.api.config.Configurable;

public class Configuration {

    @Id
    private ObjectId id;
    
    @Indexed
    private String client;
    
    private Date created;
    
    private Date modified;
    
    private Map<String,List<ComponentConfiguration>> config = new HashMap<>();
    
    public Configuration() {
        this(null);
    }
    
    @PersistenceConstructor
    public Configuration(ObjectId id) {
        this.id = id;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public String getClient() {
        return client;
    }
    
    public void setClient(String client) {
        this.client = client;
    }
    
    public Date getCreated() {
        return created;
    }
    
    public void setCreated(Date created) {
        this.created = created;
    }
    
    public Date getModified() {
        return modified;
    }
    
    public void setModified(Date modified) {
        this.modified = modified;
    }
    
    public Map<String, List<ComponentConfiguration>> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, List<ComponentConfiguration>> config) {
        this.config = config == null ? new HashMap<>() : config;
    }
    
    /**
     * Getter for the configurations for the parsed configurable component
     * @param component the configurable component
     * @return the configuration
     */ 
    //NOTE: this searches to the list of components. If we end up with a lot of config objects we
    // might want to change the model to Map<String category, Map<String type, ComponentConfiguration>>
    public <C extends ComponentConfiguration> Iterable<C> getConfigurations(Configurable<C> component){
        return getConfigurations(component, true);
    }
    @SuppressWarnings("unchecked")
    public <C extends ComponentConfiguration> Iterable<C> getConfigurations(Configurable<C> component, boolean onlyEnabled){
        if(component == null){
            return null;
        }
        List<ComponentConfiguration> catConfigs = config.get(component.getComponentCategory());
        if(catConfigs == null){
            return Collections.emptyList();
        }
        return (Iterable<C>)catConfigs.stream()
                .filter(cc -> Objects.equals(component.getComponentName(), cc.getType()))
                .filter(cc -> component.getComponentType().isAssignableFrom(cc.getClass()))
                .filter(cc -> !onlyEnabled || cc.isEnabled())
            .collect(Collectors.toList());
    }
}
