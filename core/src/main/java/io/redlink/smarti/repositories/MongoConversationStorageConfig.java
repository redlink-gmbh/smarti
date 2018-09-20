package io.redlink.smarti.repositories;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="smarti.storage.mongodb")
public class MongoConversationStorageConfig {

    /**
     * MongoBB has a 16MByte document size limit. So we need to limit the number of messages
     * stored with a conversation to ensure that we keep under that limit.
     * 
     * Limiting the number of messages to 5000 will work if we need less as 400chars to represent
     * single messages.
     */ //#2281
    public static final int DEFAULT_MAX_MESSAGES_PER_CONVERSATION = 5000;
 
    int maxConvMsg = DEFAULT_MAX_MESSAGES_PER_CONVERSATION;
    
    
    public int getMaxConvMsg() {
        return maxConvMsg;
    }
    
    public void setMaxConvMsg(int maxConvMsg) {
        this.maxConvMsg = maxConvMsg;
    }
}
