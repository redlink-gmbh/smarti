package io.redlink.smarti.repositories;

import java.util.Iterator;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

/**
 * Mongo application event listener for {@link Conversation}s that ensures that
 * save and update operations to not store {@link Conversation}s with more as
 * {@link ConversationRepository#DEFAULT_MAX_MESSAGES_PER_CONVERSATION} messages!
 * 
 * see <a href="https://github.com/redlink-gmbh/smarti/issues/281">#281</a> for details
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class ConversationRepoListener extends AbstractMongoEventListener<Conversation> {

    
    private final MongoConversationStorageConfig config;

    public ConversationRepoListener(MongoConversationStorageConfig config){
        this.config = config;
    }
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Conversation> event) {
        Conversation conversation = event.getSource();
        int numMsg = conversation.getMessages().size();
        if(numMsg > config.getMaxConvMsg()){
            int numRemove = numMsg - config.getMaxConvMsg();
            //remove messages from the front (oldest) until the limit of messages is reached
            Iterator<Message> it = conversation.getMessages().iterator();
            for(int i = 0; i < numRemove && it.hasNext(); i++){
                it.next();
                it.remove();
            }
        }
    }
}
