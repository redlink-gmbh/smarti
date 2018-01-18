package io.redlink.smarti.cloudsync;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.repositories.UpdatedIds;

import org.apache.commons.collections4.ListUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component that allows to rebuild an index of a {@link ConversationRepository}.
 * @author Rupert Westenthaler
 *
 */
@Component
public class ConversationCloudSync {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    ConversationCloudSync(ConversationRepository conversationRepository){
        this.conversationRepository = conversationRepository;
    }
    
    public SyncData syncAll(ConversytionSyncCallback callback) {
        return sync(callback, null);
    }

    public SyncData sync(ConversytionSyncCallback callback, Date since) {
        long start = System.currentTimeMillis();
        UpdatedIds updated = conversationRepository.updatedSince(since);
        Date lastUpdate = updated.getLastModified();
        AtomicInteger count = new AtomicInteger();
        //load in batches of 10 from the MongoDB
        ListUtils.partition(updated.ids(), 10).forEach(batch -> {
            conversationRepository.findAll((Iterable<ObjectId>)batch).forEach(c -> {
                    callback.updateConversation(c, lastUpdate);
                    count.incrementAndGet();
                });
        });
        return new SyncData(lastUpdate, count.get(), (int)(System.currentTimeMillis()-start));

    }
    
    public static interface ConversytionSyncCallback {
        void updateConversation(Conversation conversation, Date syncDate);
    }
    
    public static class SyncData {
        final int count;
        final int duration;
        final Date syncDate;
        SyncData(Date syncDate, int count, int duration){
            this.syncDate = syncDate;
            this.count = count;
            this.duration = duration;
        }
        
        public Date getSyncDate() {
            return syncDate;
        }
        
        public int getCount() {
            return count;
        }
        
        public int getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return "SyncData [syncDate=" + (syncDate == null ? null : syncDate.toInstant()) + ", count=" + count + ", duration=" + duration + "ms]";
        }
        
    }

}
