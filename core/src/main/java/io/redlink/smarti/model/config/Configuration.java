package io.redlink.smarti.model.config;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

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
    
}
