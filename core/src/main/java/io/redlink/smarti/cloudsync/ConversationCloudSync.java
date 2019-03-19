package io.redlink.smarti.cloudsync;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.repositories.UpdatedIds;

import org.apache.commons.collections4.ListUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Component that allows to rebuild an index of a {@link ConversationRepository}.
 * @author Rupert Westenthaler
 *
 */
@Component
@EnableConfigurationProperties(ConversationCloudSyncConfiguration.class)
public class ConversationCloudSync {
    
    final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ConversationRepository conversationRepository;
    private final ConversationCloudSyncConfiguration config;
    
    ConversationCloudSync(ConversationCloudSyncConfiguration config, ConversationRepository conversationRepository){
        this.config = config;
        this.conversationRepository = conversationRepository;
    }
    
    public SyncData syncAll(ConversytionSyncCallback callback, IndexingStatus indexingStatus) {
        return sync(callback, null, indexingStatus);
    }

    public SyncData sync(ConversytionSyncCallback callback, Date date, IndexingStatus status) {
        log.debug("cloud sync conversation Repository [date: {}, epochSize: {}, batchSize: {}, caller: {}]", 
                date == null ? null : date.toInstant(),config.getEpochSize(), config.getBatchSize(), callback);
        long start = System.currentTimeMillis();
        UpdatedIds<ObjectId> updated;
        AtomicLong updatedCount = new AtomicLong();
        AtomicLong deletedCount = new AtomicLong();
        status.setStarted(new Date());
        Date since = date;
        do {
            updated = conversationRepository.updatedSince(since, config.getEpochSize());
            final Date currentModifiedBatch = updated.getLastModified();
            status.incrementCount(updated.ids().size());
            status.setUntil(updated.getLastModified());
            //load in batches of 10 from the MongoDB
            ListUtils.partition(updated.ids(), config.getBatchSize()).forEach(batch -> {
                //NOTE: findAll will also return conversations marked as deleted
                conversationRepository.findAll((Iterable<ObjectId>)batch).forEach(c -> {
                        try {
                            if(c == null || c.getDeleted() != null){
                                callback.removeConversation(c.getId(), currentModifiedBatch);
                                deletedCount.incrementAndGet();
                                status.incrementDeleted();
                            } else {
                                callback.updateConversation(c, currentModifiedBatch);
                                updatedCount.incrementAndGet();
                                status.incrementUpdate();
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
    public static class IndexingStatus {
        
        private Date started;
        private int count;
        private Date until;
        private int updated;
        private int deleted;
        
        public Date getStarted() {
            return started;
        }
        public void incrementCount(int num) {
            count = count + num;
        }
        public void setUntil(Date until) {
            this.until = until;
        }
        public Date getUntil() {
            return until;
        }
        public void incrementUpdate() {
            updated++;
        }
        public void incrementDeleted() {
            deleted++;
        }
        public void setStarted(Date started) {
            this.started = started;
        }
        public int getCount() {
            return count;
        }
        public void setCount(int count) {
            this.count = count;
        }
        public int getUpdated() {
            return updated;
        }
        public void setUpdated(int updated) {
            this.updated = updated;
        }
        public int getDeleted() {
            return deleted;
        }
        public void setDeleted(int deleted) {
            this.deleted = deleted;
        }
        
        public int getProcessed(){
            return updated + deleted;
        }
        public double completedPercent(){
            if(count <= 0 || getProcessed() < 1){
                return 0;
            } else {
                return (new BigDecimal(getProcessed()/(double)count)
                        .setScale(2, RoundingMode.HALF_EVEN))
                        .doubleValue() * 100;
            }
        }
        
        @Override
        public String toString() {
            int duration = started == null ? 0 : (int)((new Date().getTime() - started.getTime())/1000);
            return "[started " + (duration > 119 ? (duration/60 +"min") : (duration + "sec") ) + " ago , processed=" + getProcessed() + '/' + count + '(' + completedPercent() + "% complete)]";
        }
        
        
        
    }
}
