package io.redlink.smarti.cloudsync;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.repositories.UpdatedIds;

import org.apache.commons.collections4.ListUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Component that allows to rebuild an index of a {@link ConversationRepository}.
 * @author Rupert Westenthaler
 *
 */
@Component
public class ConversationCloudSync {
    
    final Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    ConversationCloudSync(ConversationRepository conversationRepository){
        this.conversationRepository = conversationRepository;
    }
    
    public SyncData syncAll(ConversytionSyncCallback callback) {
        return sync(callback, null);
    }

    public SyncData sync(ConversytionSyncCallback callback, Date date) {
        long start = System.currentTimeMillis();
        UpdatedIds<ObjectId> updated;
        AtomicLong updatedCount = new AtomicLong();
        AtomicLong deletedCount = new AtomicLong();
        Date since = date;
        do {
            updated = conversationRepository.updatedSince(since, 10000);
            final Date currentModifiedBatch = updated.getLastModified();
            //load in batches of 10 from the MongoDB
            ListUtils.partition(updated.ids(), 10).forEach(batch -> {
                //NOTE: findAll will also return conversations marked as deleted
                conversationRepository.findAll((Iterable<ObjectId>)batch).forEach(c -> {
                        try {
                            if(c == null || c.getDeleted() != null){
                                callback.removeConversation(c.getId(), currentModifiedBatch);
                                deletedCount.incrementAndGet();
                            } else {
                                callback.updateConversation(c, currentModifiedBatch);
                                updatedCount.incrementAndGet();
                            }
                        } catch (RuntimeException e) {
                            if(log.isDebugEnabled()){
                                log.warn("Unable to update {}", c, e);
                            } else {
                                log.warn("Unable to update {} ({} - {})", c, e.getClass().getSimpleName(), e.getMessage());
                            }
                        }
                    });
            });
            since = currentModifiedBatch;
        } while(!updated.ids().isEmpty());
        return new SyncData(updated.getLastModified(), updatedCount.get(), deletedCount.get(), (int)(System.currentTimeMillis()-start));

    }
    
    public static interface ConversytionSyncCallback {
        void removeConversation(ObjectId conversationId, Date syncDate);
        void updateConversation(Conversation conversation, Date syncDate);
    }
    
    public static class SyncData {
        final long updated;
        final long deleted;
        final int duration;
        final Date syncDate;
        SyncData(Date syncDate, long updated, long deleted, int duration){
            this.syncDate = syncDate;
            this.updated = updated;
            this.deleted = deleted;
            this.duration = duration;
        }
        
        public Date getSyncDate() {
            return syncDate;
        }
        
        public long getCount() {
            return updated + deleted;
        }
        
        public long getUpdatedCount() {
            return updated;
        }
        
        public long getDeletedCount() {
            return deleted;
        }
        
        public int getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return "SyncData [syncDate=" + (syncDate == null ? null : syncDate.toInstant()) + ", updated=" + updated + ", deleted=" + deleted + ", duration=" + duration + "ms]";
        }
        
    }

}
