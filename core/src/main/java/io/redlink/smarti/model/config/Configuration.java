package io.redlink.smarti.model.config;

import java.util.Date;

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
    
    private SmartiConfiguration smarti;

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
    
    public SmartiConfiguration getSmarti() {
        return smarti;
    }
    
    public void setSmarti(SmartiConfiguration smarti) {
        this.smarti = smarti;
    }

    
    
}
