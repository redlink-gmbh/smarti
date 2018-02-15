package io.redlink.smarti.repositories;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

public class UpdatedIds<I extends Serializable> {

    private final Date lastModified;
    final private List<I> ids;

    public UpdatedIds(Date lastModified, List<I> ids){
        this.ids = ids;
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public List<I> ids(){
        return ids;
    }

    @Override
    public String toString() {
        return "UpdatedConversationIds [lastModified=" + lastModified + ", ids=" + ids + "]";
    }


}