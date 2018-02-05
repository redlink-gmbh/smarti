package io.redlink.smarti.chatpal.Service;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import io.redlink.smarti.chatpal.model.ChatpalMessage;
import io.redlink.smarti.chatpal.repo.ChatpalRepository;
import io.redlink.smarti.repositories.UpdatedIds;

@Component
public class ChatpalMessageServcie {

    private final ChatpalRepository repo;
    
    public ChatpalMessageServcie(ChatpalRepository repo) {
        this.repo = repo;
    }
    
    public void store(ObjectId clientId, Map<String,Object> data){
        repo.store(clientId, data);
    }
    
    public UpdatedIds<ObjectId> updatedSince(Date date){
        return repo.updatedSince(date);
    }
    
    public ChatpalMessage get(ObjectId id){
        return repo.findOne(id);
    }
    
    public Iterable<ChatpalMessage> get(Iterable<ObjectId> ids){
        return repo.findAll(ids);
    }
    
    public void delete(ObjectId clientId, String msgId){
        repo.markAsDeleted(clientId, msgId);
    }
    
    public void deleteClientMessages(ObjectId clientId){
        repo.markAsDeleted(clientId);
    }
    
}
