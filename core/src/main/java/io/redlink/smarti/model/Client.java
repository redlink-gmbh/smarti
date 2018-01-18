package io.redlink.smarti.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
@Document
public class Client {

    @Id
    @ApiModelProperty(position = 0, notes="the id of the client", readOnly=true)
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @ApiModelProperty(position = 1,notes="The name of the client", required=true, allowEmptyValue=false, example="demo-client")
    @Indexed(unique=true)
    private String name;

    @ApiModelProperty(position = 2,notes="An optional  description for the client", required=false, allowEmptyValue=true,
            example="Dieser Client wird nur für die Demo verwendet.")
    private String description;

    @ApiModelProperty(position = 3,notes="the date/time of the last update", readOnly=true)
    private Date lastUpdate;

    @ApiModelProperty(position = 4,notes="allows to mark this client as the default", hidden=true)
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Client other = (Client) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Client [id=" + id + ", name=" + name + ", lastUpdate=" 
                + (lastUpdate == null ? null : lastUpdate.toInstant()) + ", defaultClient=" + defaultClient + "]";
    }
    
    
}
