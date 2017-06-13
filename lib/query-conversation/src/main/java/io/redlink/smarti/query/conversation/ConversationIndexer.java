package io.redlink.smarti.query.conversation;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.api.event.StoreServiceEvent;
import io.redlink.smarti.api.event.StoreServiceEvent.Operation;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.ConversationMeta.Status;
import io.redlink.smarti.model.Message;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;

@Component
public class ConversationIndexer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int DEFAULT_COMMIT_WITHIN = 10*1000; //10sec

    public static final int MIN_COMMIT_WITHIN = 1000; //1sec

    @Value("${smarti.index.conversation.commitWithin:0}") //<0 ... use default
    private int commitWithin = DEFAULT_COMMIT_WITHIN; 

    @Value("${smarti.index.conversation.message.merge-timeout:30}")
    private int messageMergeTimeout = 30;

    @Autowired
    @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX)
    private SolrCoreDescriptor conversationCore;
    
    private final SolrCoreContainer solrServer;
    
    private final StoreService storeService;
    
    @Autowired
    public ConversationIndexer(SolrCoreContainer solrServer, StoreService storeService){
        this.solrServer = solrServer;
        this.storeService = storeService;
    }
    
    @PostConstruct
    protected void init() throws IOException, SolrServerException {
        if(commitWithin <= 0){
            commitWithin = DEFAULT_COMMIT_WITHIN;
        } else if(commitWithin < MIN_COMMIT_WITHIN){
            commitWithin = MIN_COMMIT_WITHIN;
        }
        log.info("initialize VocabularyIndex after startup ...");
        final Date since;
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            SolrQuery query = new SolrQuery("*:*");
            query.addSort(FIELD_MODIFIED, ORDER.desc);
            query.setFields(FIELD_MODIFIED);
            query.setRows(1);
            query.setStart(0);
            QueryResponse result = solr.query(query);
            if(result.getResults() == null || result.getResults().getNumFound() < 1){
                since = null;
            } else {
                since = (Date)result.getResults().get(0).getFieldValue(FIELD_MODIFIED);
            }
            final long startTime = System.currentTimeMillis();
            final AtomicLong idxCount = new AtomicLong(0);
            //batch processing
            Iterators.partition(storeService.listConversationIDs().iterator(), 100).forEachRemaining(
                    ids -> {
                        List<SolrInputDocument> docs = ids.stream()
                                .map(storeService::get)
                                //FIXME: the following checks should be parameters of the storeService#listConversationIDs() call
                                .filter(c -> since == null || since.before(c.getLastModified()))
                                .filter(c -> c.getMeta().getStatus() == ConversationMeta.Status.Complete)
                                .map(this::toSolrInputDocument)
                                .collect(Collectors.toList());
                        if (!docs.isEmpty()) {
                            try {
                                solr.add(docs, commitWithin);
                                idxCount.addAndGet(docs.size());
                            } catch (SolrServerException | IOException e) {
                                log.warn("Could not execute batch index", e); //TODO throw error or not?
                            }
                        }
                    }
            );
            solr.commit();
            log.info("Indexed {} Conversations from StoreService in {}ms", idxCount.get(), System.currentTimeMillis() - startTime);

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
        if(storeEvent.getOperation() == Operation.SAVE){
            if(storeEvent.getConversationStatus() == Status.Complete){
                indexConversation(storeService.get(storeEvent.getConversationId()), true);
            } //else we do not index uncompleted conversations
        } else if(storeEvent.getOperation() == Operation.DELETE){
            removeConversation(storeEvent.getConversationId(), true);
        }
    }
    
    public void removeConversation(Conversation conversation, boolean commit) {
        removeConversation(conversation.getId(), commit);
    }
    public void removeConversation(ObjectId conversationId, boolean commit) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            solr.deleteByQuery(String.format("%s:%s OR %s:%s", FIELD_ID, conversationId.toHexString(), 
                    FIELD_CONVERSATION_ID, conversationId.toHexString()),commitWithin);
            if(commit){
                solr.commit();
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversationId, e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    public void indexConversation(Conversation conversation, boolean commit) {
        try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
            solr.add(toSolrInputDocument(conversation), commitWithin);
            if(commit){
                solr.commit();
            }
        } catch (IOException | SolrServerException e) {
            log.warn("Unable to index Conversation {} ({}: {})",conversation.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("STACKTRACE",e);
        }        
    }

    private SolrInputDocument toSolrInputDocument(Conversation conversation) {
        final SolrInputDocument solrConversation = new SolrInputDocument();

        solrConversation.setField(FIELD_ID, conversation.getId().toHexString());
        solrConversation.setField(FIELD_TYPE, "conversation");
        solrConversation.setField(FIELD_MODIFIED, conversation.getLastModified());
        addContextFields(solrConversation, conversation);

        solrConversation.setField(FIELD_MESSAGE_COUNT, conversation.getMessages().size());
        if(!conversation.getMessages().isEmpty()) {
            solrConversation.setField(FIELD_START_TIME, conversation.getMessages().get(0).getTime());
            solrConversation.setField(FIELD_END_TIME, Iterables.getLast(conversation.getMessages()).getTime());

            List<SolrInputDocument> messages = new ArrayList<>(conversation.getMessages().size());
            Message prevMessage = null;
            SolrInputDocument prevSolrInputDoc = null;
            for (int i = 0; i < conversation.getMessages().size(); i++) {
                final Message m = conversation.getMessages().get(i);
                if (!m.isPrivate()) {
                    if ((prevMessage != null) && (prevSolrInputDoc != null)
                            // Same user
                            && Objects.equals(m.getUser(), prevMessage.getUser())
                            // "same" user
                            && Objects.equals(m.getOrigin(), prevMessage.getOrigin())
                            // within X seconds
                            && m.getTime().before(DateUtils.addSeconds(prevMessage.getTime(), messageMergeTimeout))) {
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
        solrMsg.setField(FIELD_MESSAGE_ID, message.getId());
        solrMsg.setField(FIELD_MESSAGE_IDX, i);
        solrMsg.setField(FIELD_TYPE, "message");
        if (message.getUser() != null) {
            solrMsg.setField(FIELD_USER_ID, message.getUser().getId());
            solrMsg.setField(FIELD_USER_NAME, message.getUser().getDisplayName());
        }
        addContextFields(solrMsg, conversation);

        solrMsg.setField(FIELD_MESSAGE, message.getContent());
        solrMsg.setField(FIELD_TIME, message.getTime());
        solrMsg.setField(FIELD_VOTE, message.getVotes());

        // TODO: Add keywords, links, ...

        return solrMsg;
    }

    private void addContextFields(final SolrInputDocument solrDoc, Conversation conversation) {
        if (conversation.getContext() != null) {
            solrDoc.setField(FIELD_CONTEXT, conversation.getContext().getContextType());
            solrDoc.setField(FIELD_ENVIRONMENT, conversation.getContext().getEnvironmentType());
            solrDoc.setField(FIELD_DOMAIN, conversation.getContext().getDomain());
        }
    }
    
    private SolrInputDocument mergeSolrUInputDoc(SolrInputDocument prev, SolrInputDocument current) {
        prev.setField(FIELD_MESSAGE, String.format("%s%n%s", prev.getFieldValue(FIELD_MESSAGE), current.getFieldValue(FIELD_MESSAGE)));
        prev.setField(FIELD_VOTE, Integer.parseInt(String.valueOf(prev.getFieldValue(FIELD_VOTE))) + Integer.parseInt(String.valueOf(current.getFieldValue(FIELD_VOTE))));
        return prev;
    }


}
