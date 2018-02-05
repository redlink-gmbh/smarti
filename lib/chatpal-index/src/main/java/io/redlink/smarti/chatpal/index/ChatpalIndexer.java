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

package io.redlink.smarti.chatpal.index;

import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.CHATPAL_INDEX;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_CLIENT;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_ID;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_INDEX_VERSION;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.FIELD_SYNC_DATE;
import static io.redlink.smarti.chatpal.index.ChatpalIndexConfiguration.INDEX_VERSION;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
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

import io.redlink.smarti.chatpal.Service.ChatpalMessageServcie;
import io.redlink.smarti.chatpal.model.ChatpalMessage;
import io.redlink.smarti.repositories.UpdatedIds;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

@Component
@EnableScheduling
public class ChatpalIndexer {


    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int DEFAULT_COMMIT_WITHIN = 10*1000; //10sec

    public static final int MIN_COMMIT_WITHIN = 1000; //1sec
    
    @Value("${smarti.index.chatpal.commitWithin:0}") //<0 ... use default
    private int commitWithin = DEFAULT_COMMIT_WITHIN; 

    @Autowired
    @Qualifier(CHATPAL_INDEX)
    private SolrCoreDescriptor chatpalCore;
    
    @Autowired(required=false)
    private ChatpalMessageServcie chatpalService;
    
    private final SolrCoreContainer solrServer;
    
    @Value("${smarti.index.rebuildOnStartup:false}")
    private boolean rebuildOnStartup = false;

    private final ExecutorService indexerPool;

    private IndexingTask indexingTask;

    @Autowired
    public ChatpalIndexer(SolrCoreContainer solrServer, ChatpalMessageServcie chatpalService){
        this.solrServer = solrServer;
        this.chatpalService = chatpalService;
        this.indexerPool = Executors.newSingleThreadExecutor(
                new BasicThreadFactory.Builder().namingPattern("chatpal-indexing-thread-%d").daemon(true).build());
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
        log.info("sync chatpal index on startup");
        log.info("initialize ChatpalIndex after startup ...");
        indexingTask = new IndexingTask();
        Date syncDate = null; //null triggers a full rebuild (default)
        if(!rebuildOnStartup){
            try (SolrClient solr = solrServer.getSolrClient(chatpalCore)){
                //search for messages indexed with an earlier version of the index
                SolrQuery query = new SolrQuery("*:*");
                query.addFilterQuery(String.format("!%s:%s",FIELD_INDEX_VERSION,INDEX_VERSION));
                query.setRows(0); //we only need the count
                if(solr.query(query).getResults().getNumFound()  > 0){
                    log.info("Chatpal index contains documents indexed with an outdated version - full re-build required");
                } else { //partial update possible. Search for the last sync date ...
                    query = new SolrQuery("*:*");
                    query.addSort(FIELD_SYNC_DATE, ORDER.desc);
                    query.setFields(FIELD_SYNC_DATE);
                    query.setRows(1);
                    query.setStart(0);
                    QueryResponse result = solr.query(query);
                    if(result.getResults() != null && result.getResults().getNumFound() > 0){
                        syncDate = (Date)result.getResults().get(0).getFieldValue(FIELD_SYNC_DATE);
                        log.info("Perform partial update of Chatpal index (lastSync date:{})", syncDate);
                    }
                }
            } catch (IOException | SolrServerException e) {
                log.warn("Updating Chatpal index on startup failed ({} - {})", e.getClass().getSimpleName(), e.getMessage());
                log.debug("STACKTRACE:",e);
            }
        } else {
            log.info("full re-build on startup required via configuration");
        }
        indexingTask.setLastSync(syncDate);
        indexerPool.execute(indexingTask);
    }    
    
    
    public int getCommitWithin() {
        return commitWithin;
    }
    
    public void removeMessage(ChatpalMessage message) {
        try (SolrClient solr = solrServer.getSolrClient(chatpalCore)){
            solr.deleteByQuery(getDeleteQuery(message),commitWithin);
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to delete {} form index",message, e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }
    public void indexMessage(ChatpalMessage message, Date syncDate){
        try (SolrClient solr = solrServer.getSolrClient(chatpalCore)){
            SolrInputDocument doc = toSolrInputDocument(message);
            if(doc == null){ //filter this document
                solr.deleteByQuery(getDeleteQuery(message),commitWithin);
            } else {
                if(syncDate != null){
                    doc.setField(FIELD_SYNC_DATE, syncDate);
                }
                solr.add(doc,commitWithin);
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to delete {} form index",message, e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    private String getDeleteQuery(ChatpalMessage message) {
        return String.format("%s:%s", FIELD_ID, message.getId().toHexString());
    }

    private SolrInputDocument toSolrInputDocument(ChatpalMessage message) {
        if(message.isRemoved()){
            return null; //delete from index
        }
        final SolrInputDocument doc = new SolrInputDocument();

        doc.setField(FIELD_ID, message.getId().toHexString());
        //add the index version so that we can check schema changes on startup
        doc.setField(FIELD_INDEX_VERSION, INDEX_VERSION);
        doc.setField(FIELD_CLIENT, message.getClient().toHexString());
        message.getData().entrySet().forEach(e -> {
            doc.setField(e.getKey(), e.getValue());
        });
        return doc;
    }


    @Scheduled(initialDelay=15*1000,fixedDelay=15*1000)
    public void syncIndex() {
        Instant now = Instant.now();
        log.debug("sync Chatpal index with Repository");
        if(indexingTask.isCompleted()) {
            indexerPool.execute(indexingTask);
        } else if(indexingTask.isActive()){
            log.info("skipping Term Index sync at {} as previouse task has not yet completed!", now);
        } else {
            log.info("previouse sync of Term Index is still enqueued at {}!", now);
        }
    }
    
//    @Scheduled(cron = "30 0 3 * * *" /* once per day at 03:00:30 AM */)
//    public void rebuildIndex() {
//        log.info("starting scheduled full sync of the Chatpal index");
//        indexingTask.enqueueFullRebuild(); //enqueue a full rebuild
//        if(indexingTask.isCompleted()) {
//            indexerPool.execute(indexingTask); //and start it when not running
//        } else if(indexingTask.isActive()){ //when running the full rebuild will be done on the next run
//            log.info("enqueued full term index rebuild as an update is currently running");
//        }
//    }
    
    private class IndexingTask implements Runnable {

        final Lock lock = new ReentrantLock();
        
        AtomicBoolean active = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);;
        
        Date lastSync;
        
        boolean fullRebuild = false;
        
        public boolean isActive() {
            return active.get();
        }
        
        public boolean isCompleted() {
            return completed.get();
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
                    log.info("Unable to set lastSync to {} as the term indexer is currently active!",
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
        
        @Override
        public void run() {
            active.set(true);
            try {
                do {
                    final Date nextSync;
                    lock.lock();
                    try {
                        if(fullRebuild){
                            lastSync = null;
                            fullRebuild = false;
                        }
                    } finally {
                        lock.unlock();
                    }
                    SyncData syncData;
                    if(lastSync == null){
                        log.debug("start full rebuild of Index");
                        syncData = syncAll();
                    } else {
                        if(log.isTraceEnabled()){
                            log.trace("update Index with changes after {}", lastSync == null ? null : lastSync.toInstant());
                        }
                        syncData = sync(lastSync);
                    }
                    if(syncData.getCount() > 0){
                        log.debug("updated Chatpal Index - {}", syncData);
                    } else {
                        log.debug("no update for Chatpal Index - {}", syncData);
                    }
                    lock.lock();
                    try {
                        lastSync = syncData.getSyncDate();
                        completed.set(true);
                    } finally {
                        lock.unlock();
                    }
                } while (fullRebuild); //another fill rebuild scheduled?
            } finally {
                active.set(false);
            }
        }
        
        private SyncData syncAll() {
            return sync(null);
        }
        private SyncData sync(Date since) {
            long start = System.currentTimeMillis();
            UpdatedIds<ObjectId> updated = chatpalService.updatedSince(since);
            Date lastUpdate = updated.getLastModified();
            AtomicInteger count = new AtomicInteger();
            //load in batches of 10 from the MongoDB
            ListUtils.partition(updated.ids(), 10).forEach(batch -> {
                chatpalService.get(batch).forEach(c -> {
                        indexMessage(c, lastUpdate);
                        count.incrementAndGet();
                    });
            });
            return new SyncData(lastUpdate, count.get(), (int)(System.currentTimeMillis()-start));

        }
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
