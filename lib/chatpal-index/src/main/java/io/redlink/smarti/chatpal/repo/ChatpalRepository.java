package io.redlink.smarti.chatpal.repo;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import io.redlink.smarti.chatpal.model.ChatpalMessage;

public interface ChatpalRepository extends CrudRepository<ChatpalMessage, ObjectId>, ChatpalRepositoryCustom {
    
    String CHATPAL_COLLECTION = "chatpal";

    
}
