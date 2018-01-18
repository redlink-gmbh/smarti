package io.redlink.smarti.chatpal.model;

import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.PersistenceConstructor;

public class ChatpalMessage {

    
    private final ObjectId id;
    private String msgId;
    private ObjectId client;
    private boolean removed = false;
    Map<String,Object> data;
    
    @PersistenceConstructor
    protected ChatpalMessage(ObjectId id){
        this.id = id;
    }

    public ObjectId getId() {
        return id;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public ObjectId getClient() {
        return client;
    }

    public void setClient(ObjectId client) {
        this.client = client;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
    
    @Override
    public String toString() {
        return "ChatpalMessage [id=" + id + ", client=" + client + ", removed=" + removed + ", data=" + data + "]";
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
        ChatpalMessage other = (ChatpalMessage) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    
}
