package io.redlink.smarti.chatpal.repo;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import io.redlink.smarti.repositories.UpdatedIds;

public interface ChatpalRepositoryCustom {

    void store(ObjectId client, Map<String, Object> chatpalMessage);

    void delete(ObjectId client, String messageIdx);
    
    UpdatedIds<ObjectId> updatedSince(Date date);

}