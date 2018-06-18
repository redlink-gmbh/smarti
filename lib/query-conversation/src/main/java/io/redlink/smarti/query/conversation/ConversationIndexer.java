/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.query.conversation;

import static io.redlink.smarti.model.Message.Metadata.SKIP_ANALYSIS;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.api.event.StoreServiceEvent.Operation;
import io.redlink.smarti.cloudsync.ConversationCloudSync;
import io.redlink.smarti.cloudsync.ConversationCloudSync.ConversytionSyncCallback;
import io.redlink.smarti.cloudsync.ConversationCloudSync.SyncData;
import io.redlink.smarti.model.Context;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.ConversationMeta.Status;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Metadata;
import io.redlink.smarti.services.ConversationService;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

@Component
@EnableScheduling
public class ConversationIndexer implements ConversytionSyncCallback {


    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int DEFAULT_COMMIT_WITHIN = 10*1000; //10sec

    public static final int MIN_COMMIT_WITHIN = 1000; //1sec
    
    //TODO: make configurable
    private static final Set<String> NOT_INDEXED_META_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ConversationMeta.PROP_TOKEN))); //do not index the users token

    //TODO: make configurable
    private static final Set<String> NOT_INDEXED_ENVIRONMENT_FIELDS = Collections.emptySet();
    
    /*
     * Constants defining the context for messages
     */
    //We target more as 100 chars are context
    protected static final int MIN_CONTEXT_LENGTH = 100;
    //We try to optimize for about 300 chars of context
    protected static final int CONTEXT_LENGTH = 300;
    //Include at least the last two messages (the current and an other)
    protected static final int MIN_INCL_MSGS = 2;
    //Include a maximum of 10 messages
    protected static final int MAX_INCL_MSGS = 10;
    //include at least one message before the current one
    protected static final int MIN_INCL_BEFORE = 1;
    //include at least one message after the current one
    protected static final int MIN_INCL_AFTER = 1;
    //Include at least all messages of the last 3 minutes
    protected static final long MIN_AGE = TimeUnit.MINUTES.toMillis(3);
    //Include no messages older as a day (except for MIN_INCL_BEFORE and MIN_INCL_AFTER)
    protected static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);


    @Value("${smarti.index.conversation.commitWithin:0}") //<0 ... use default
    private int commitWithin = DEFAULT_COMMIT_WITHIN; 

    @Value("${smarti.index.conversation.message.merge-timeout:30}")
    private int messageMergeTimeout = 30;

    @Autowired
    @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX)
    private SolrCoreDescriptor conversationCore;
    
    @Autowired(required=false)
    private ConversationCloudSync cloudSync;
    
    private ConversationIndexTask indexTask;

    private final SolrCoreContainer solrServer;
    
    private final ConversationService conversationService;

    private final ExecutorService indexerPool;

    @Value("${smarti.index.rebuildOnStartup:false}")
    private boolean rebuildOnStartup = false;

    @Autowired
    public ConversationIndexer(SolrCoreContainer solrServer, ConversationService storeService){
        this.solrServer = solrServer;
        this.conversationService = storeService;
        this.indexerPool = Executors.newSingleThreadExecutor(
                new BasicThreadFactory.Builder().namingPattern("conversation-indexing-thread-%d").daemon(true).build());
    }
    
    @PostConstruct
    protected void init()  {
        if(commitWithin <= 0){
            commitWithin = DEFAULT_COMMIT_WITHIN;
        } else if(commitWithin < MIN_COMMIT_WITHIN){
            commitWithin = MIN_COMMIT_WITHIN;
        }
    }
    
    @EventListener(ContextRefreshedEvent.class)
    protected void startup() {
        log.info("sync conversation index on startup");
        indexTask = cloudSync == null ? null : new ConversationIndexTask(cloudSync);
        if(indexTask != null){
            log.info("initialize ConversationIndex after startup ...");
            Date syncDate = null; //null triggers a full rebuild (default)
            if(!rebuildOnStartup){
                try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
                    //search for conversations indexed with an earlier version of the index
                    SolrQuery query = new SolrQuery("*:*");
                    query.addFilterQuery(String.format("!%s:%s",FIELD_INDEX_VERSION,CONVERSATION_INDEX_VERSION));
                    query.setRows(0); //we only need the count
                    if(solr.query(query).getResults().getNumFound()  > 0){
                        log.info("conversation index contains documents indexed with an outdated version - full re-build required");
                        solr.deleteByQuery("*:*");
                        solr.commit();
                        solr.optimize(); //required as some schema changes will cause exceptions without this on reindexing
                    } else { //partial update possible. Search for the last sync date ...
                        query = new SolrQuery("*:*");
                        query.addSort(FIELD_SYNC_DATE, ORDER.desc);
                        query.setFields(FIELD_SYNC_DATE);
                        query.setRows(1);
                        query.setStart(0);
                        QueryResponse result = solr.query(query);
                        if(result.getResults() != null && result.getResults().getNumFound() > 0){
                            syncDate = (Date)result.getResults().get(0).getFieldValue(FIELD_SYNC_DATE);
                            log.info("Perform partial update of conversation index (lastSync date:{})", syncDate);
                        }
                    }
                } catch (IOException | SolrServerException e) {
                    log.warn("Updating Conversation index on startup failed ({} - {})", e.getClass().getSimpleName(), e.getMessage());
                    log.debug("STACKTRACE:",e);
                }
            } else {
                log.info("full re-build on startup required via configuration");
            }
            indexTask.setLastSync(syncDate);
            indexerPool.execute(indexTask);
        } else { //manual initialization (performs a full re-index to be up-to-date)
            Iterators.partition(conversationService.listConversationIDs().iterator(), 100).forEachRemaining(
                    ids -> {
                        ids.stream()
                                .map(conversationService::getConversation)
                                .forEach(c -> indexConversation(c, false));
                    });
        }
    }    
    
    
    public int getCommitWithin() {
        return commitWithin;
    }
    
    /**
     * Processes update events as e.g. sent by the {@link StoreService}
     * @param storeEvent
     */
    @EventListener
    protected void conversationUpdated(StoreServiceEvent storeEvent){
        log.debug("StoreServiceEvent for {}", storeEvent.getConversationId());
        if(storeEvent.getOperation() == Operation.SAVE){
            if(storeEvent.getConversationStatus() == Status.Complete){
                log.debug("  - SAVE operation of a COMPLETED conversation[id: {}]", storeEvent.getConversationId());
                indexConversation(conversationService.getConversation(storeEvent.getConversationId()), true);
            } //else we do not index uncompleted conversations
        } else if(storeEvent.getOperation() == Operation.DELETE){
            log.debug("  - DELETE operation for conversation[id:{}]", storeEvent.getConversationId());
            removeConversation(storeEvent.getConversationId(), true);
        } else {
            log.debug("  - {} ignored", storeEvent);
        }
    }
    
    public void removeConversation(Conversation conversation, boolean commit) {
        removeConversation(conversation.getId(), commit);
    }
    @Override
    public void removeConversation(ObjectId conversationId, Date syncDate) {
        removeConversation(conversationId, false);
    }
    public void removeConversation(ObjectId conversationId, boolean commit) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            solr.deleteByQuery(getDeleteQuery(conversationId), commitWithin);
            if(commit){
                solr.commit();
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversationId, e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }
    
    @Override
    public void updateConversation(Conversation conversation, Date syncDate) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            SolrInputDocument doc = toSolrInputDocument(conversation);
            if(doc != null){
                doc.setField(FIELD_SYNC_DATE, syncDate);
                solr.add(doc, commitWithin);
            } else { //remove from index
                solr.deleteByQuery(getDeleteQuery(conversation),commitWithin);
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    public void indexConversation(Conversation conversation, boolean commit) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            SolrInputDocument doc = toSolrInputDocument(conversation);
            if(doc != null){
                solr.add(doc, commitWithin);
            } else { //remove from index
                solr.deleteByQuery(getDeleteQuery(conversation),commitWithin);
            }
            if(commit){
                solr.commit();
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    private String getDeleteQuery(Conversation conversation) {
        return getDeleteQuery(conversation.getId());
    }

    private String getDeleteQuery(ObjectId conversationId) {
        return String.format("%s:%s OR %s:%s", FIELD_ID, conversationId.toHexString(), 
                FIELD_CONVERSATION_ID, conversationId.toHexString());
    }

    private SolrInputDocument toSolrInputDocument(Conversation conversation) {
        final SolrInputDocument solrConversation = new SolrInputDocument();

        solrConversation.setField(FIELD_ID, conversation.getId().toHexString());
        //#150 index the current version of the index so that we can detect the need of a
        //full re-index after a software update on startup
        solrConversation.setField(FIELD_INDEX_VERSION, CONVERSATION_INDEX_VERSION);
        solrConversation.setField(FIELD_COMPLETED, conversation.getMeta().getStatus() == ConversationMeta.Status.Complete);
        solrConversation.setField(FIELD_TYPE, TYPE_CONVERSATION);
        solrConversation.setField(FIELD_MODIFIED, conversation.getLastModified());
        
        //add owner and context information
        if(conversation.getOwner() != null){ //conversations without owner SHOULD NOT exist, but we found a NPE in a log ...
            solrConversation.setField(FIELD_OWNER, conversation.getOwner().toHexString());
        }
        addContextFields(solrConversation, conversation);

        solrConversation.setField(FIELD_MESSAGE_COUNT, conversation.getMessages().size());
        if(!conversation.getMessages().isEmpty()) {
            solrConversation.setField(FIELD_START_TIME, conversation.getMessages().get(0).getTime());
            solrConversation.setField(FIELD_END_TIME, conversation.getMessages().get(conversation.getMessages().size() -1).getTime());

            List<SolrInputDocument> messages = new ArrayList<>(conversation.getMessages().size());
            Message prevMessage = null;
            SolrInputDocument prevSolrInputDoc = null;
            for (int i = 0; i < conversation.getMessages().size(); i++) {
                final Message m = conversation.getMessages().get(i);
                if (!m.isPrivate() && !MapUtils.getBoolean(m.getMetadata(), SKIP_ANALYSIS, false)) {
                    if ((prevMessage != null) && (prevSolrInputDoc != null)
                            && Objects.equals(m.getUser(), prevMessage.getUser()) // Same user
                            && Objects.equals(m.getOrigin(), prevMessage.getOrigin()) // "same" user
                            && m.getTime().before(DateUtils.addSeconds(prevMessage.getTime(), messageMergeTimeout))) { // within X seconds
                        // merge messages;
                        prevSolrInputDoc = mergeSolrUInputDoc(prevSolrInputDoc, toSolrInputDocument(m, i, conversation));

                        messages.remove(messages.size() - 1);
                    } else {
                        prevSolrInputDoc = toSolrInputDocument(m, i, conversation);
                    }
                    messages.add(prevSolrInputDoc);
                    prevMessage = m;

                }
            }
            messages.forEach(m -> {
                //we want the content of the messages also stored with the conversation (e.g. for highlighting)
                solrConversation.addField(FIELD_MESSAGES, m.getFieldValues(FIELD_MESSAGE));
                //in addition store the content of the conversation also in the Solr MLT field
                solrConversation.addField(FIELD_MLT_CONTEXT, m.getFieldValues(FIELD_MESSAGE));
            });
            solrConversation.addChildDocuments(messages);
        }

        return solrConversation;
    }


    
    private SolrInputDocument toSolrInputDocument(Message message, int i, Conversation conversation) {
        final SolrInputDocument solrMsg = new SolrInputDocument();
        String id = new StringBuilder(conversation.getId().toHexString()).append('_')
                //we prefer to use the messageId but some system might not provide a such so we have a fallback
                //to the index within the conversation
                .append(StringUtils.isNoneBlank(message.getId()) ? message.getId() : String.valueOf(i)).toString();
        solrMsg.setField(FIELD_ID, id);
        solrMsg.setField(FIELD_CONVERSATION_ID, conversation.getId());
        solrMsg.setField(FIELD_MESSAGE_IDS, message.getId());
        solrMsg.setField(FIELD_MESSAGE_IDXS, i);
        //#150 index the current version of the index so that we can detect the need of a
        //full re-index after a software update on startup
        solrMsg.setField(FIELD_INDEX_VERSION, CONVERSATION_INDEX_VERSION);
        solrMsg.setField(FIELD_COMPLETED, conversation.getMeta().getStatus() == ConversationMeta.Status.Complete);
        solrMsg.setField(FIELD_TYPE, TYPE_MESSAGE);
        if (message.getUser() != null) {
            solrMsg.setField(FIELD_USER_ID, message.getUser().getId());
            solrMsg.setField(FIELD_USER_NAME, message.getUser().getDisplayName());
        }

        //add owner and context information
        solrMsg.setField(FIELD_OWNER, conversation.getOwner().toHexString());
        addContextFields(solrMsg, conversation);

        solrMsg.setField(FIELD_MESSAGE, message.getContent());
        solrMsg.setField(FIELD_TIME, message.getTime());
        solrMsg.setField(FIELD_VOTE, message.getVotes());

        //message context (for context based similarity search)
        int[] context = ConversationContextUtils.getMessageContext(conversation.getMessages(), i, 
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_INCL_BEFORE, MIN_INCL_AFTER, 
                MIN_AGE, MAX_AGE);
        solrMsg.setField(FIELD_MESSAGE_CONTEXT_START, context[0]);
        solrMsg.setField(FIELD_MESSAGE_CONTEXT_END, context[1]);
        for(Message ctxMsg : conversation.getMessages().subList(context[0], context[1])){
            if(!ctxMsg.isPrivate()){
                solrMsg.addField(FIELD_MLT_CONTEXT, ctxMsg.getContent());
            } //else do not use private messages - not even as context!
        }
        
        // TODO: Add keywords, links, ...

        return solrMsg;
    }
    
    private void addContextFields(final SolrInputDocument solrDoc, Conversation conversation) {
        if (conversation.getContext() != null) {
            Context ctx = conversation.getContext();
            solrDoc.setField(FIELD_CONTEXT, ctx.getContextType());
            solrDoc.setField(FIELD_ENVIRONMENT, ctx.getEnvironmentType());
            solrDoc.setField(FIELD_DOMAIN, ctx.getDomain());
            if(ctx.getEnvironment() != null){
                ctx.getEnvironment().entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> StringUtils.isNotBlank(e.getKey()) && !e.getValue().isEmpty())
                .filter(e -> !NOT_INDEXED_ENVIRONMENT_FIELDS.contains(e.getKey()))
                .forEach(e -> {
                    solrDoc.setField(getEnvironmentField(e.getKey()), e.getValue());
                });
            }
        }
        if(conversation.getMeta() != null){
            conversation.getMeta().getProperties().entrySet().stream()
                    .filter(e -> Objects.nonNull(e.getValue()))
                    .filter(e -> StringUtils.isNotBlank(e.getKey()) && !e.getValue().isEmpty())
                    .filter(e -> !NOT_INDEXED_META_FIELDS.contains(e.getKey()))
                    .forEach(e -> {
                        solrDoc.setField(getMetaField(e.getKey()), e.getValue());
                    });
        }
    }

    private SolrInputDocument mergeSolrUInputDoc(SolrInputDocument prev, SolrInputDocument current) {
        prev.setField(FIELD_MESSAGE, String.format("%s%n%s", prev.getFieldValue(FIELD_MESSAGE), current.getFieldValue(FIELD_MESSAGE)));
        prev.addField(FIELD_MESSAGE_IDS, current.getFieldValue(FIELD_MESSAGE_IDS));
        prev.addField(FIELD_MESSAGE_IDXS, current.getFieldValue(FIELD_MESSAGE_IDXS));
        prev.setField(FIELD_VOTE, Integer.parseInt(String.valueOf(prev.getFieldValue(FIELD_VOTE))) + Integer.parseInt(String.valueOf(current.getFieldValue(FIELD_VOTE))));
        return prev;
    }

    @Scheduled(initialDelay=15*1000,fixedDelay=15*1000)
    public void syncIndex() {
        Instant now = Instant.now();
        if(indexTask != null){
            if(indexTask.isError()){
                if(log.isDebugEnabled()){
                    log.warn("ConversationIndex in ErrorState (last completed Sync: {}, {})", 
                            indexTask.getLastSync() == null ? "none" : indexTask.getLastSync().toInstant(), 
                            indexTask.getError(), indexTask.getException());
                } else {
                    log.warn("ConversationIndex in ErrorState (last completed Sync: {}, {})", 
                            indexTask.getLastSync() == null ? "none" : indexTask.getLastSync().toInstant(), 
                            indexTask.getError());
                }
            }
            if(!indexTask.isActive()) {
                log.debug("execute sync of conversation index with repository (last completed Sync: {})",
                        indexTask.getLastSync() == null ? "none" : indexTask.getLastSync().toInstant());
                indexerPool.execute(indexTask);
            } else {  
                log.info("skipping conversation index sync at {} as indexing is currently active (last completed sync: {})",
                        now, indexTask.getLastSync() == null ? "none" : indexTask.getLastSync().toInstant());
            }
        }
    }
    
    @Scheduled(cron = "30 0 3 * * *" /* once per day at 03:00:30 AM */)
    public void rebuildIndex() {
        if(indexTask != null){
            log.info("starting scheduled full sync of the conversation index");
            indexTask.enqueueFullRebuild(); //enqueue a full rebuild
            if(!indexTask.isActive()){
                indexerPool.execute(indexTask); //and start it when not running
            } else { //when running the full rebuild will be done on the next run
                log.info("enqueued full Conversation index rebuild as an update is currently running");
            }
        } else { //no cloud sync active. So re-index via the store service
            log.info("starting scheduled full rebuild of the conversation index");
            Iterators.partition(conversationService.listConversationIDs().iterator(), 100).forEachRemaining(
                    ids -> {
                        ids.stream()
                                .map(conversationService::getConversation)
                                .forEach(c -> indexConversation(c, false));
                    });
        }
    }
    
    private class ConversationIndexTask implements Runnable {

        final ConversationCloudSync cloudSync;
        final Lock lock = new ReentrantLock();
        
        AtomicBoolean active = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);;
        
        Date lastSync;
        
        boolean fullRebuild = false;
        private Exception error;
        
        ConversationIndexTask(ConversationCloudSync cloudSync) {
            this.cloudSync = cloudSync;
        }
        
        public boolean isActive() {
            return active.get();
        }
        
        public boolean isCompleted() {
            return completed.get();
        }
        
        public boolean isError(){
            return error != null;
        }
        
        public String getError(){
            Exception error = getException();
            return error == null ? null : String.format("%s: %s", error.getClass().getSimpleName(), error.getMessage());
        }
        
        public Exception getException(){
            return error;
        }
        
        public Date getLastSync() {
            return lastSync;
        }
        /**
         * Enqueues a full rebuild of the index. Can also be called if the
         * Indexer is currently active
         */
        public void enqueueFullRebuild(){
            lock.lock();
            try {
                fullRebuild = true;
            } finally {
                lock.unlock();
            }
        }
        /**
         * Setter for the lastSync time. Can only be used if not {@link #isActive() active}
         * @param lastSync the time or <code>null</code> to to a full rebuild
         * @return <code>true</code> if the pased date was set or <code>false</code> if the
         * date could not be set because the indexer is currently {@link #isActive() active}
         * @see #enqueueFullRebuild() 
         */
        public boolean setLastSync(Date lastSync) {
            lock.lock();
            try {
                if(Objects.equals(this.lastSync, lastSync)){
                    return true;
                } else if(active.get()){
                    log.info("Unable to set lastSync to {} as the conversation indexer is currently active!",
                            lastSync == null ? null : lastSync.toInstant());
                    return false;
                } else {
                    this.lastSync = lastSync;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        
        public ConversationCloudSync getCloudSync() {
            return cloudSync;
        }
        
        @Override
        public void run() {
            active.set(true);
            try {
                final Date nextSync;
                lock.lock();
                try {
                    completed.set(false);
                    error = null;
                    if(fullRebuild){
                        lastSync = null;
                        fullRebuild = false;
                    }
                } finally {
                    lock.unlock();
                }
                SyncData syncData;
                if(lastSync == null){
                    log.debug("start full rebuild of Index using {}", cloudSync);
                    syncData = cloudSync.syncAll(ConversationIndexer.this);
                    try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
                        log.debug("optimize Index after the full rebuild");
                        solr.optimize(); //optimize after a full rebuild
                    } catch (IOException | SolrServerException e) {/* ignore*/}
                } else {
                    if(log.isTraceEnabled()){
                        log.trace("update Index with changes after {}", lastSync == null ? null : lastSync.toInstant());
                    }
                    syncData = cloudSync.sync(ConversationIndexer.this, lastSync);
                }
                if(syncData.getCount() > 0){
                    log.debug("performed {} updates in the Conversation Index - {}", syncData.getCount(), syncData);
                } else {
                    log.debug("no update for Conversation Index - {}", syncData);
                }
                lock.lock();
                try {
                    lastSync = syncData.getSyncDate();
                    completed.set(true);
                } finally {
                    lock.unlock();
                }
            } catch (final Exception e) {
                this.error = e;
                throw e;
            } finally {
                active.set(false);
            }
        }
        
    }
    
}
