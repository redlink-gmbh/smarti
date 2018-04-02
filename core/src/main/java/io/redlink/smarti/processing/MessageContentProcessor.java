package io.redlink.smarti.processing;

import org.bson.types.ObjectId;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;


public interface MessageContentProcessor {

    String processMessageContent(ObjectId clientId, Conversation conversation, Message message);
    
}
