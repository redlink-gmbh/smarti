package io.redlink.smarti.chatpal.repo;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import io.redlink.smarti.repositories.UpdatedIds;

public interface ChatpalRepositoryCustom {

    void store(ObjectId client, Map<String, Object> chatpalMessage);

    void markAsDeleted(ObjectId client, String messageIdx);
    void markAsDeleted(ObjectId client);
    
    UpdatedIds<ObjectId> updatedSince(Date date);

}