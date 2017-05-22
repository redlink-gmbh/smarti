package io.redlink.nlp.model.entitylinking;

import java.util.HashSet;
import java.util.Set;

public class LinkedEntity {

    
    public String id;
    public String uri;
    public String title;
    public Set<String> types;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Set<String> getTypes() {
        return types;
    }
    public void addType(String type){
        if(type == null){
            return;
        }
        if(this.types == null){
            this.types = new HashSet<>();
        }
        this.types.add(type);
    }
    public void setTypes(Set<String> types) {
        this.types = types;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
        LinkedEntity other = (LinkedEntity) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "LinkedEntity [id=" + id + ", title=" + title + ", types=" + types + 
                (uri != null ?", uri=" + uri : "")+ "]";
    }

    
    
}
