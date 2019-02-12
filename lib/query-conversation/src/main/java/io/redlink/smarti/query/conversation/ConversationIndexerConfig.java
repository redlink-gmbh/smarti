package io.redlink.smarti.query.conversation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronTrigger;

@ConfigurationProperties(prefix = "smarti.index.conversation")
public class ConversationIndexerConfig{

    public static final int DEFAULT_COMMIT_WITHIN = 10*1000; //10sec
    public static final int MIN_COMMIT_WITHIN = 1000; //1sec
    
    public static final int DEFAULT_MESSAGE_MERGE_TIMEOUT = 30;

    public static final int DEFAULT_SYNC_DELAY = 15 * 1000; //15 sec
    public static final int MIN_SYNC_DELAY = 5 * 1000; //5sec
    
    private int commitWithin = DEFAULT_COMMIT_WITHIN;
    private ConversationIndexerConfig.Message message = new Message();
    
    private CronTrigger reindexCron = null;
    private int syncDelay = DEFAULT_SYNC_DELAY;
    
    public static class Message {
        private int mergeTimeout = DEFAULT_MESSAGE_MERGE_TIMEOUT;
        
        public void setMergeTimeout(int mergeTimeout) {
            this.mergeTimeout = mergeTimeout < 0 ? DEFAULT_MESSAGE_MERGE_TIMEOUT : mergeTimeout;
        }
        
        public int getMergeTimeout() {
            return mergeTimeout;
        }
    }
    
    public void setCommitWithin(int commitWithin) {
        this.commitWithin = commitWithin <= 0 ? DEFAULT_COMMIT_WITHIN : 
            commitWithin < MIN_COMMIT_WITHIN ? MIN_COMMIT_WITHIN : commitWithin;
    }
    
    public int getCommitWithin() {
        return commitWithin;
    }
    
    public void setMessage(ConversationIndexerConfig.Message message) {
        this.message = message;
    }
    
    public ConversationIndexerConfig.Message getMessage() {
        return message;
    }
    
    public void setReindexCron(CronTrigger reindexCron) {
        this.reindexCron = reindexCron;
    }
    
    public CronTrigger getReindexCron() {
        return reindexCron;
    }
    
    public void setSyncDelay(int syncDelay) {
        this.syncDelay = syncDelay <= 0 ? DEFAULT_SYNC_DELAY : 
            syncDelay < MIN_SYNC_DELAY ? MIN_SYNC_DELAY : syncDelay;
    }
    
    public int getSyncDelay() {
        return syncDelay;
    }
}