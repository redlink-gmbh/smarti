package io.redlink.smarti.query.conversation;

import static io.redlink.smarti.model.Message.Metadata.SKIP_ANALYSIS;

import java.util.Objects;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronTrigger;

import io.redlink.smarti.model.Message.Metadata;

@ConfigurationProperties(prefix = "smarti.index.conversation")
public class ConversationIndexerConfig{

    public static final int DEFAULT_COMMIT_WITHIN = 10*1000; //10sec
    public static final int MIN_COMMIT_WITHIN = 1000; //1sec
    
    public static final int DEFAULT_MESSAGE_MERGE_TIMEOUT = 30;

    public static final int DEFAULT_SYNC_DELAY = 15 * 1000; //15 sec
    public static final int MIN_SYNC_DELAY = 5 * 1000; //5sec

    public static final CronTrigger DEFAULT_SYNC_CRON = new CronTrigger("16 0 2 * * *");//once per day at 02:00:16 AM )

    /**
     * By default the last 50 messages are used as context for a conversation
     */
    public static final int DEFAULT_CONVERSATION_CONTEXT_SIZE = 50;
    
    private int commitWithin = DEFAULT_COMMIT_WITHIN;
    private ConversationIndexerConfig.Message message = new Message();
    
    private CronTrigger syncCron = DEFAULT_SYNC_CRON;
    private CronTrigger reindexCron = null;
    private int syncDelay = DEFAULT_SYNC_DELAY;
    
    private int convCtxSize = DEFAULT_CONVERSATION_CONTEXT_SIZE;
    private Boolean embedded;
    
    
    public static class Message {
        private int mergeTimeout = DEFAULT_MESSAGE_MERGE_TIMEOUT;
        private boolean indexPrivate = false;
        
        public void setMergeTimeout(int mergeTimeout) {
            this.mergeTimeout = mergeTimeout < 0 ? DEFAULT_MESSAGE_MERGE_TIMEOUT : mergeTimeout;
        }
        
        public int getMergeTimeout() {
            return mergeTimeout;
        }
        
        public boolean isIndexPrivate() {
            return indexPrivate;
        }
        
        public void setIndexPrivate(boolean indexPrivate) {
            this.indexPrivate = indexPrivate;
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
    /**
     * Getter for the number of messages used a context for a conversation.
     * <p>
     * The text of the latest messages is used as context for the conversation.
     * This context is indexed with the conversation for content based recommendations.
     * @return the number of messages used as context for the conversation
     */
    public int getConvCtxSize() {
        return convCtxSize;
    }
    /**
     * Setter for the number of messages used as context for the conversation
     * <p>
     * The text of the latest messages is used as context for the conversation.
     * This context is indexed with the conversation for content based recommendations.
     * @param convCtxSize the number of messages in the context. 
     * <code>0</code> to deactivate this feature; <code>&lt;0</code> to use the
     * {@link #DEFAULT_CONVERSATION_CONTEXT_SIZE default}
     */
    public void setConvCtxSize(int convCtxSize) {
        this.convCtxSize = convCtxSize < 0 ? DEFAULT_CONVERSATION_CONTEXT_SIZE : convCtxSize;
    }
    
    /**
     * Returns <code>true</code> if the parsed message is indexed or <code>false</code> if
     * it should not be indexed because its private (and indexing of private messages is disabled) 
     * or {@link Metadata#SKIP_ANALYSIS} is set
     * @param msg the message to check
     * @return the state: <code>true</code> to index <code>false</code> to skip
     */
    public boolean isMessageIndexed(io.redlink.smarti.model.Message msg) {
        return (!msg.isPrivate() || getMessage().isIndexPrivate()) && !MapUtils.getBoolean(msg.getMetadata(), SKIP_ANALYSIS, false);
    }
    /**
     * Checks if two message should be indexed into a single Solr {@link Document} as they where
     * sent by the same user within a short period of time
     * @param prev
     * @param current
     * @return
     */
    public boolean isMessageMerged(io.redlink.smarti.model.Message prev, io.redlink.smarti.model.Message current) {
        return prev != null && current != null &&
                Objects.equals(current.getUser(), prev.getUser()) &&// Same user
                Objects.equals(current.getOrigin(), prev.getOrigin()) &&// "same" user
                current.getTime().before(DateUtils.addSeconds(prev.getTime(), getMessage().getMergeTimeout()));
    }

    /**
     * When using an external Solr Index (Standalone or Cloud) the index
     * will be synced with the MongoDB based on this cron. <p>
     * This means that all Conversations updated after the last sync will
     * be indexed as part of this Task. This is different to the
     * {@link #setReindexCron(CronTrigger)} where the whole index is
     * rebuilt.
     * @param syncCron the cron configuration or <code>null</code> to
     * deactuvate this feature
     * @since 0.9.0
     */
    public void setSyncCron(CronTrigger syncCron) {
        this.syncCron = syncCron;
    }
    
    /**
     * When using an external Solr Index (Standalone or Cloud) the index
     * will be synced with the MongoDB based on this cron. <p>
     * This means that all Conversations updated after the last sync will
     * be indexed as part of this Task. This is different to the
     * {@link #getReindexCron()} where the whole index is
     * rebuilt.
     * @return
     * @since 0.9.0
     */
    public CronTrigger getSyncCron() {
        return syncCron;
    }

    /**
     * Allows to explicitly configure embedded mode. Embedded mode indicates
     * that every Smarti instance uses its own Solr Server for managing the
     * conversation index. Typically this means running an embedded Solr server
     * in the same JVM, but one could also configure an different external Solr
     * server for each smarti instance.<p>
     * The default <code>null</code> will do an automatic detection based on the
     * type of the {@link SolrClient}.
     * @param embedded the embedded mode. <code>true</code> for embedded, 
     * <code>false</code> for shared external or <code>null</code> for autodetect
     */
    public void setEmbedded(Boolean embedded) {
        this.embedded = embedded;
    }
    
    /**
     * Allows to explicitly configure embedded mode. Embedded mode indicates
     * that every Smarti instance uses its own Solr Server for managing the
     * conversation index. Typically this means running an embedded Solr server
     * in the same JVM, but one could also configure an different external Solr
     * server for each smarti instance.<p>
     * The default <code>null</code> will do an automatic detection based on the
     * type of the {@link SolrClient}.
     * @return the embedded mode. <code>true</code> for embedded, 
     * <code>false</code> for shared external or <code>null</code> for autodetect
     */
    public Boolean getEmbedded() {
        return embedded;
    }

    
}