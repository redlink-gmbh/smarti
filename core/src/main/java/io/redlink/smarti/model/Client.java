package io.redlink.smarti.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.redlink.smarti.model.config.Configuration;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Date;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
public class Client {

    @Id
    @ApiModelProperty(position = 0)
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Indexed(unique=true)
    private String name;

    private String description;

    private Date lastUpdate;

    private boolean defaultClient;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isDefaultClient() {
        return defaultClient;
    }

    public void setDefaultClient(boolean defaultClient) {
        this.defaultClient = defaultClient;
    }
}
