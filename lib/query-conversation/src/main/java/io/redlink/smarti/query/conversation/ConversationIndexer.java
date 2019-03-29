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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterators;

import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.api.event.StoreServiceEvent.Operation;
import io.redlink.smarti.cloudsync.ConversationCloudSync;
import io.redlink.smarti.cloudsync.ConversationCloudSync.ConversytionSyncCallback;
import io.redlink.smarti.cloudsync.ConversationCloudSync.SyncData;
import io.redlink.smarti.cloudsync.ConversationCloudSync.IndexingStatus;
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
@EnableConfigurationProperties(ConversationIndexerConfig.class)
public class ConversationIndexer implements ConversytionSyncCallback {


    private final Logger log = LoggerFactory.getLogger(getClass());
    
    
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


    @Autowired
    @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX)
    private SolrCoreDescriptor conversationCore;
    
    @Autowired(required=false)
    private ConversationCloudSync cloudSync;
    
    private ConversationIndexTask indexTask;

    protected final SolrCoreContainer solrServer;
    
    protected final ConversationService conversationService;

    protected final TaskScheduler taskScheduler;
    
    protected final ExecutorService indexerPool;

    @Value("${smarti.index.rebuildOnStartup:false}")
    private boolean rebuildOnStartup = false;

    protected final ConversationIndexerConfig indexConfig;
    
    @Autowired
    public ConversationIndexer(ConversationIndexerConfig config, SolrCoreContainer solrServer, ConversationService storeService, 
            TaskScheduler taskScheduler){
        this.indexConfig = config;
        this.solrServer = solrServer;
        this.conversationService = storeService;
        this.taskScheduler = taskScheduler;
        this.indexerPool = Executors.newSingleThreadExecutor(
                new BasicThreadFactory.Builder().namingPattern("conversation-indexing-thread-%d").daemon(true).build());
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
        
        //start the recurring scheduled tasks
        this.taskScheduler.scheduleAtFixedRate(this::syncIndex, indexConfig.getSyncDelay());
        if(indexConfig.getReindexCron() != null){
            log.info("Rebuild Index Cron: ", indexConfig.getReindexCron());
            this.taskScheduler.schedule(this::rebuildIndex, indexConfig.getReindexCron());
        } else {
            log.info("Rebuild Index Cron: disabled");
        }
        
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
            solr.deleteByQuery(getConversationDeleteQuery(conversationId), indexConfig.getCommitWithin());
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
            indexConversationAndMessages(solr, conversation, syncDate);
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    public void indexConversation(Conversation conversation, boolean commit) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            indexConversationAndMessages(solr, conversation, null);
            if(commit){
                solr.commit();
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    private void indexConversationAndMessages(SolrClient solr, Conversation conversation, Date syncDate)
            throws SolrServerException, IOException {
        SolrInputDocument doc = toSolrInputDocument(conversation, syncDate);
        if(doc != null){ //we want to index this conversation
            List<String> convCtx = indexMessages(solr, conversation, syncDate);
            if(CollectionUtils.isEmpty(convCtx)) {
                //we want the content of the messages also stored with the conversation 
                //(e.g. for highlighting, content based recommendations ...)
                doc.addField(FIELD_MESSAGES, convCtx);
            }
            solr.add(doc, indexConfig.getCommitWithin());
        } else { //remove from conversation AND all messages from the index
            solr.deleteByQuery(getConversationDeleteQuery(conversation), indexConfig.getCommitWithin());
        }
    }
    /**
     * Getter for the delete query that deletes all {@link SolrDocument}s 
     * for the parsed conversation (incl. docs for messages and the conversation itself
     * Messages of the parsed conversation
     * @param conversation
     * @return
     */
    private String getConversationDeleteQuery(Conversation conversation) {
        return getConversationDeleteQuery(conversation.getId());
    }

    /**
     * Getter for the delete query that deletes all {@link SolrDocument}s 
     * for the parsed conversation (incl. docs for messages and the conversation itself
     * Messages of the parsed conversation id
     * @param conversation
     * @return
     */
    private String getConversationDeleteQuery(ObjectId conversationId) {
        return String.format("%s:%s OR %s:%s", 
                FIELD_ID, conversationId.toHexString(), 
                FIELD_CONVERSATION_ID, conversationId.toHexString());
    }
    /**
     * Getter for the delete query that deletes all {@link SolrDocument}s for
     * Messages of the parsed conversation
     * @param conversation
     * @return
     */
    private String getMessagesDeleteQuery(Conversation conversation) {
        return getMessagesDeleteQuery(conversation.getId());
    }
    /**
     * Getter for the delete query that deletes all {@link SolrDocument}s for
     * Messages of the parsed conversation id
     * @param conversation
     * @return
     */
    private String getMessagesDeleteQuery(ObjectId conversationId) {
        return String.format("%s:%s AND (%s:%s OR %s:%s)", 
                FIELD_TYPE, TYPE_MESSAGE,
                FIELD_ID, conversationId.toHexString(), 
                FIELD_CONVERSATION_ID, conversationId.toHexString());
    }

    private SolrInputDocument toSolrInputDocument(Conversation conversation, Date syncDate) {
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
        }
        if(syncDate != null) {
            solrConversation.setField(FIELD_SYNC_DATE, syncDate);
        }

        return solrConversation;
    }

    /**
     * Indexes the messages of the conversation in their own solr documents and returns the
     * text of the {@link ConversationIndexerConfig#getConvCtxSize()} messages to be used
     * later as contextual content when indexing the conversation
     * @param client the solr client used to index the messages
     * @param conversation the conversation
     * @return the {@link ConversationIndexerConfig#getConvCtxSize()} messages to be used
     * later as contextual content when indexing the conversation
     */
    private List<String> indexMessages(SolrClient client, Conversation conversation, Date syncDate) {
        //(1) delete all messages for this conversation first (so that we do not keep anthem messages in the index)
        try {
            client.deleteByQuery(getMessagesDeleteQuery(conversation));
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to delete messages of Conversation {} before reindexing ({}: {})", conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }
        AtomicInteger idx = new AtomicInteger();
        return ListUtils.union( //we need the sections and append messages.size() to also index the remainder
                ConversationContextUtils.getContextSections(indexConfig, conversation.getMessages().iterator(),
                    0, //we do not have any offset as we process all messages at once
                    MIN_CONTEXT_LENGTH, CONTEXT_LENGTH,MIN_INCL_MSGS, MAX_INCL_MSGS),
                Collections.singletonList(conversation.getMessages().size())).stream()
            .filter(secEnd -> secEnd > idx.get()) //filter empty sections
            .flatMap(secEnd -> {
                int startIdx = idx.getAndSet(secEnd);
                return indexSection(client, conversation, conversation.getMessages().subList(startIdx, secEnd), startIdx, secEnd, syncDate);
            })
            .map(doc -> doc.getFieldValue(FIELD_MESSAGE))
            .filter(Objects::nonNull).map(String::valueOf)
            .collect(lastN(indexConfig.getConvCtxSize()));
    }
    
    private Stream<SolrInputDocument> indexSection(SolrClient client, Conversation conversation, List<Message> section, int startIdx, int endIdx, Date syncDate){
        Set<String> msgDocIds = new HashSet<>();
        Set<String> msgIds = new HashSet<>();
        List<String> context = new LinkedList<String>();
        AtomicInteger idx = new AtomicInteger(startIdx);
        String sectionId = conversation.getId().toHexString() + '_' + startIdx + '-' + endIdx;
        List<Pair<Integer,Message>> toIndex = section.stream()
            .map(m -> new ImmutablePair<>(idx.getAndIncrement(), m))
            .filter(p -> indexConfig.isMessageIndexed(p.right))
            .map(p -> {
                msgDocIds.add(getSolrDocId(conversation, p.getRight()));
                if(p.getRight().getId() != null) {
                    msgIds.add(p.getRight().getId());
                }
                if(StringUtils.isNotBlank(p.getRight().getContent())) {
                    context.add(p.getRight().getContent());
                }
                return p;
            })
            .collect(Collectors.toCollection(LinkedList::new)); //collect as we need the contextual information
        toIndex.add(null); //add an EOS indicator
        
        //Index Documents and merge messages within this section!
        MutablePair<Message, SolrInputDocument> prev = new MutablePair<>();
        return toIndex.stream()
            .map(current -> { //filter messages we can merge with the previous
                final SolrInputDocument cur;
                if(current != null && indexConfig.isMessageMerged(prev.left, current.getRight())) { // merged?
                    mergeSolrInputDoc(prev.right, current.getLeft(), current.getRight());
                    cur = null;
                } else { //includes current == null (EOS)
                    cur = prev.right; //the prev. SolrDoc is complete ... index it
                    if(current != null) { //not EOS ...
                        //create a new SolrInputDocument for this message
                        prev.right = toSolrInputDocument(current.getRight(), current.getLeft(), conversation, syncDate);
                        //add fields for the message context
                        prev.right.setField(FIELD_MESSAGE_CONTEXT_START, startIdx);
                        prev.right.setField(FIELD_MESSAGE_CONTEXT_END, endIdx);
                        prev.right.setField(FIELD_MESSAGE_CONTEXT, context);
                        prev.right.setField(FIELD_MESSAGE_CONTEXT_IDS, msgDocIds);
                        prev.right.setField(FIELD_MESSAGE_CONTEXT_MSG_IDS, msgIds);
                        prev.right.setField(FIELD_MESSAGE_CONTEXT_ID, sectionId);
                    }
                }
                if(current != null) { //Not EOS
                    prev.left = current.getRight();
                }
                return cur;
            })
            .filter(Objects::nonNull) //filter null values present for the first or merged documents
            .map(doc -> { //index the documents (as side effect)
                try {
                    client.add(doc, indexConfig.getCommitWithin());
                    return doc;
                } catch (IOException | SolrServerException e) {
                    log.warn("Unable to index Message {} of Conversation {} ({}: {})", doc.getFieldValue(FIELD_MESSAGE_IDS), conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
                    log.debug("STACKTRACE",e);
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }
    
    private SolrInputDocument toSolrInputDocument(Message message, int idx, Conversation conv, Date syncDate) {
        if(message == null) {
            return null;
        }
        final SolrInputDocument solrMsg = new SolrInputDocument();
        solrMsg.setField(FIELD_ID, getSolrDocId(conv, message));
        solrMsg.setField(FIELD_CONVERSATION_ID, conv.getId().toHexString());
        solrMsg.setField(FIELD_MESSAGE_IDS, message.getId());
        solrMsg.setField(FIELD_MESSAGE_IDXS, idx);
        solrMsg.setField(FIELD_MESSAGE_IDX_START, idx); //the start idx (used for sorting
        //#150 index the current version of the index so that we can detect the need of a
        //full re-index after a software update on startup
        solrMsg.setField(FIELD_INDEX_VERSION, CONVERSATION_INDEX_VERSION);
        solrMsg.setField(FIELD_COMPLETED, conv.getMeta().getStatus() == ConversationMeta.Status.Complete);
        solrMsg.setField(FIELD_TYPE, TYPE_MESSAGE);
        if (message.getUser() != null) {
            solrMsg.setField(FIELD_USER_ID, message.getUser().getId());
            solrMsg.setField(FIELD_USER_NAME, message.getUser().getDisplayName());
        }

        //add owner and context information
        solrMsg.setField(FIELD_OWNER, conv.getOwner().toHexString());
        addContextFields(solrMsg, conv);

        solrMsg.setField(FIELD_MESSAGE, message.getContent());
        solrMsg.setField(FIELD_TIME, message.getTime());
        solrMsg.setField(FIELD_VOTE, message.getVotes());

        //message context (for context based similarity search)

        // TODO: Add keywords, links, ...

        if(syncDate != null) {
            solrMsg.setField(FIELD_SYNC_DATE, syncDate);
        }
        
        return solrMsg;
    }

    public static String getSolrDocId(Conversation conversation, Message message) {
        return new StringBuilder(conversation.getId().toHexString()).append('_').append(message.getId()).toString();
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

    private SolrInputDocument mergeSolrInputDoc(SolrInputDocument prev, int curIdx, Message curMsg) {
        prev.setField(FIELD_MESSAGE, String.format("%s%n%s", prev.getFieldValue(FIELD_MESSAGE), curMsg.getContent()));
        prev.addField(FIELD_MESSAGE_IDS, curMsg.getId());
        prev.addField(FIELD_MESSAGE_IDXS, curIdx);
        prev.setField(FIELD_VOTE, (Integer)prev.getFieldValue(FIELD_VOTE) + curMsg.getVotes());
        return prev;
    }

    protected void syncIndex() {
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
                log.info("ongoing indexing task (status: {}, last completed sync: {})",
                        indexTask.getIndexingStatus() == null ? "not available" : indexTask.getIndexingStatus(), 
                        indexTask.getLastSync() == null ? "none" : indexTask.getLastSync().toInstant());
            }
        }
    }
    
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
        AtomicBoolean completed = new AtomicBoolean(false);
        IndexingStatus indexingStatus = null;
        
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
        
        public IndexingStatus getIndexingStatus() {
            return indexingStatus;
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
                indexingStatus = new IndexingStatus();
                if(lastSync == null){
                    log.debug("start full rebuild of Index using {}", cloudSync);
                    syncData = cloudSync.syncAll(ConversationIndexer.this, indexingStatus);
                    try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
                        log.debug("optimize Index after the full rebuild");
                        solr.optimize(); //optimize after a full rebuild
                    } catch (IOException | SolrServerException e) {/* ignore*/}
                } else {
                    if(log.isTraceEnabled()){
                        log.trace("update Index with changes after {}", lastSync == null ? null : lastSync.toInstant());
                    }
                    syncData = cloudSync.sync(ConversationIndexer.this, lastSync, indexingStatus);
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
                    indexingStatus = null;
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
    /*
     * see https://stackoverflow.com/a/30477722/3932024
     */
    public static <T> Collector<T, ?, List<T>> lastN(int n) {
        return Collector.<T, Deque<T>, List<T>>of(ArrayDeque::new, (acc, t) -> {
            if(acc.size() == n)
                acc.pollFirst();
            acc.add(t);
        }, (acc1, acc2) -> {
            while(acc2.size() < n && !acc1.isEmpty()) {
                acc2.addFirst(acc1.pollLast());
            }
            return acc2;
        }, ArrayList::new);
    }
}
