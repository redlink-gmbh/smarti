package io.redlink.smarti.services;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.redlink.smarti.events.AnalysisCompleteEvent;
import io.redlink.smarti.exception.BadArgumentException;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.SearchResult;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.repositories.AnalysisRepository;
import io.redlink.smarti.util.ConversationUtils;

@Service
public class AnalysisService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final AnalysisRepository analysisRepo;
    private final ExecutorService processingExecutor;
    protected final ApplicationEventPublisher eventPublisher;
    protected final PrepareService prepareService;
    protected final TemplateService templateService;
    protected final QueryBuilderService queryBuilderService;
    private final ConfigurationService confService;
    private final ClientService clientService;

    
    private final Map<AnalysisKey, CompletableFuture<Analysis>> processing = new HashMap<>();
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Holds {@link Analysis} results for some time to <ol>
     * <li> cache Analysis not stored in the {@link AnalysisRepository}
     * <li> have fast access to {@link Analysis} in subsequent requests
     * </ol>
     */
    protected final Cache<Map.Entry<ObjectId, ObjectId>, Analysis> analysisCache = CacheBuilder.newBuilder()
            .maximumSize(1000) //TODO: make configureable
            .expireAfterWrite(1, TimeUnit.MINUTES) //initally do not cache for long
            .expireAfterAccess(10, TimeUnit.MINUTES) //if we see read requests cache for longer
            .build();
    
    public AnalysisService(AnalysisRepository analysisRepo, 
            Optional<ExecutorService> processingExecutor, Optional<ApplicationEventPublisher> eventPublisher,
            PrepareService prepareService, TemplateService templateService, QueryBuilderService queryBuilderService,
            ConfigurationService confService, ClientService clientService) {
        this.analysisRepo = analysisRepo;
        this.processingExecutor = processingExecutor.orElseGet(() -> Executors.newFixedThreadPool(2));
        this.eventPublisher = eventPublisher.orElse(null);
        this.prepareService = prepareService;
        this.templateService = templateService;
        this.queryBuilderService = queryBuilderService;
        this.confService = confService;
        this.clientService = clientService;
    }
    /**
     * Analyzes the parsed conversation and returns a Future on the results.
     * If the {@link Analysis} is present a {@link CompletableFuture#completedFuture(Object) completed Future}
     * is returned.
     * @param con the conversation to be analyzed by using the configuration of its owner
     * @return the future on the results
     */
    public CompletableFuture<Analysis> analyze(Conversation con){
        return analyze(null, con);
    }
    /**
     * Analyzes the parsed conversation and returns a Future on the results.
     * If the {@link Analysis} is present a {@link CompletableFuture#completedFuture(Object) completed Future}
     * is returned.
     * @param con the conversation to be analyzed by using the configuration of its owner
     * @param parsedAnalysis if an existing analysis should be used to re-build templates and queries or <code>null</code> 
     * to analyse the parsed conversation
     * @return the future on the results
     */
    public CompletableFuture<Analysis> analyze(Conversation con, Analysis parsedAnalysis){
        return analyze(null, con, parsedAnalysis);
    }
    
    /**
     * Analyzes the parsed conversation by using the configuration of the parsed Client. Returns a Future on the results.
     * If the {@link Analysis} is present a {@link CompletableFuture#completedFuture(Object) completed Future}
     * is returned.
     * @param client the client to analyze the conversation for. If <code>null</code> the owner of the conversation 
     * is used as client
     * @param con the conversation to be analyzed by using the configuration of its owner
     * @return the future on the results
     */
    public CompletableFuture<Analysis> analyze(Client client, Conversation con){
        return analyze(client, con, null);
    }
    /**
     * Analyzes the parsed conversation by using the configuration of the parsed Client. Returns a Future on the results.
     * If the {@link Analysis} is present a {@link CompletableFuture#completedFuture(Object) completed Future}
     * is returned.
     * @param client the client to analyze the conversation for. If <code>null</code> the owner of the conversation 
     * is used as client
     * @param con the conversation to be analyzed by using the configuration of its owner
     * @param parsedAnalysis if an existing analysis should be used to re-build templates and queries or <code>null</code> 
     * to analyse the parsed conversation
     * @return the future on the results
     */
    public CompletableFuture<Analysis> analyze(Client client, Conversation con, Analysis parsedAnalysis){
        if(con == null || con.getId() == null || con.getOwner() == null){
            throw new BadArgumentException("conversation", "The conversation MUST NOT be NULL and MUST HAVE an 'id' and an 'owner'");
        }
        if(client == null){
            client = clientService.get(con.getOwner());
        }
        if(client == null){
            throw new NotFoundException(Client.class, con.getOwner(), "Client for Owner of Conversation[id=" + con.getId() + "] not found!");
        }
        AnalysisKey key = new AnalysisKey(con, getConfig(client, con));
        if(parsedAnalysis == null){
            Analysis present = getAnalysisIfPresent(key);
            if(present != null){
                return CompletableFuture.completedFuture(present);
            } else {
                return process(key, client, con, null);
            }
        } else {
            return process(key, client, con, parsedAnalysis);
        }
    }
    /**
     * Getter for the Analysis for the parsed Conversation
     * @param con the conversation
     * @return the {@link Analysis} or <code>null</code> if not available
     */
    public Analysis getAnalysisIfPresent(Conversation con){
        return getAnalysisIfPresent(null, con);
    }
    /**
     * Getter for the Analysis for the parsed Conversation using the configuration of the parsed Client
     * @param client the client
     * @param con the conversation
     * @return the {@link Analysis} or <code>null</code> if not present
     */
    public Analysis getAnalysisIfPresent(Client client, Conversation con){
        if(con == null || con.getId() == null || con.getOwner() == null){
            throw new BadArgumentException("conversation", "The conversation MUST NOT ne NULL and MUST HAVE an 'id' and an 'owner'");
        }
        if(client == null){
            client = clientService.get(con.getOwner());
        }
        if(client == null){
            throw new NotFoundException(Client.class, con.getOwner(), "Client for Owner of Conversation[id=" + con.getId() + "] not found!");
        }
        return getAnalysisIfPresent(new AnalysisKey(con, getConfig(client, con)));
    }
    
    /*
     * TODO: provide async version of the inline result methods
     */
    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Template template, String creator) throws IOException {
        return getInlineResults(client, conversation, null, template, creator, new LinkedMultiValueMap<>());
    }

    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Template template, String creator, MultiValueMap<String, String> params) throws IOException {
        return getInlineResults(client, conversation, null, template, creator, params);
    } 
    
    /*
     * TODO: provide async version of the inline result methods
     */
    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Analysis analysis, Template template, String creator) throws IOException {
        return getInlineResults(client, conversation, template, creator, new LinkedMultiValueMap<>());
    }

    public SearchResult<? extends Result> getInlineResults(Client client, Conversation conversation, Analysis analysis, Template template, String creator, MultiValueMap<String, String> params) throws IOException {
        if(analysis == null){
            try {
                analysis = analyze(client, conversation).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException e) {
                if(e.getCause() instanceof RuntimeException){
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
        return queryBuilderService.execute(client, creator, template, conversation, analysis, params);
    }


    
    protected final Analysis getAnalysisIfPresent(AnalysisKey key){
        //first look in the cache
        lock.readLock().lock();
        try {
            Analysis analysis = analysisCache.getIfPresent(key.getEntry());
            if(analysis != null && Objects.equals(analysis.getDate(), key.getDate())){
                return analysis;
            } //else cached Analysis has a different version
        } finally {
            lock.readLock().unlock();
        }
        //look in the Repository
        return analysisRepo.findByClientAndConversationAndDate(key.getClient(), key.getConversation(), key.getDate());
    }

    private CompletableFuture<Analysis> process(final AnalysisKey key, Client client, Conversation conversation, Analysis parsedAnalysis) {
        CompletableFuture<Analysis> future;
        if(parsedAnalysis == null){
            lock.readLock().lock();
            try { //look for an existing in an read lock
                future = processing.get(key);
            } finally {
                lock.readLock().unlock();
            }
            if(future != null){
                return future; //already processing :)
            }
        } else {
            future = null;
        }
        lock.writeLock().lock();
        try { //look again for an existing in an write lock
            if(parsedAnalysis != null){
                future = processing.get(key); //try to find 
            }
            if(future == null){ //we need a new processing task
                future = CompletableFuture.supplyAsync(() -> {
                    long start = System.currentTimeMillis();
                    final Analysis analysis;
                    if(parsedAnalysis == null){
                        log.trace("process {}", key);
                        analysis = prepareService.prepare(client, conversation, key.getDate());
                    } else {
                        analysis = parsedAnalysis;
                    }
                    final long processed = System.currentTimeMillis();
                    log.trace("build templates for {}", key);
                    templateService.updateTemplates(client, conversation, analysis);
                    long tempatesBuilt = System.currentTimeMillis(); 
                    log.trace("build queries for {}", key);
                    queryBuilderService.buildQueries(client, conversation, analysis);
                    long queryBuilt = System.currentTimeMillis();
                    if(log.isDebugEnabled()){
                        log.debug("analysed {} in {}ms ({}, templates: {}ms, queries: {}ms)",
                                key, queryBuilt-start, parsedAnalysis != null ? "no processing" : ("processing: " + (processed - start) + "ms"),
                                tempatesBuilt-processed, queryBuilt-tempatesBuilt);
                        ConversationUtils.logConversation(log, conversation, analysis);
                    }
                    return analysis;
                }, processingExecutor)
                    .thenApply(analysis -> { //on success we want to persist some analysis and also notify with application evnets
                        if(client != null && Objects.equals(conversation.getOwner(), analysis.getClient())){
                            analysis = analysisRepo.updateAnalysis(analysis);
                        } //else we do not cache analysis results for clients different as the owner of the conversation
                        publishEvent(new AnalysisCompleteEventImpl(client,conversation, key.getDate(), analysis));
                        return analysis;
                    })
                    .whenComplete((analysis , exception) -> { //finally update the caches 
                        lock.writeLock().lock();
                        try {
                            //(2) remove from futureMap (regardless of the result)
                            processing.remove(key);
                            //(3) update the in-memory cache (if successful)
                            if(parsedAnalysis == null && //do not cache results for parsed analysis!!
                                    analysis != null){
                                //do not override cached value with an older analysis
                                Analysis cachedAnalysis = analysisCache.getIfPresent(key.getEntry());
                                analysisCache.put(key.getEntry(), //always put as expireAfterWrite << expireAfterAccess
                                        cachedAnalysis == null || cachedAnalysis.getDate().before(analysis.getDate()) ?
                                        analysis : cachedAnalysis);
                            }
                        } finally {
                            lock.writeLock().unlock();
                        }
                    });                
                processing.put(key, future);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return future;
    }
    /**
     * Getter for the Analysis with the parsed <code>id</code> as stored in the Repository
     * @param id the id
     * @return the Analysis or <code>null</code> if not found
     */
    public Analysis getAnalysis(ObjectId id){
        return analysisRepo.findOne(id);
    }
    /**
     * Getter for all Analysis stored for a given conversation and Client
     * @param conversationId
     * @return
     */
    public List<Analysis> getAnalysisForConversation(ObjectId clientId, ObjectId conversationId){
        return analysisRepo.findByClientAndConversation(clientId, conversationId);
    }
    
    protected final Configuration getConfig(Client client, final Conversation conversation) {
        Configuration config;
        if(client == null){
            config = confService.getClientConfiguration(conversation.getOwner());
        } else {
            config = confService.getClientConfiguration(client);
        }
        if(config == null){
            log.trace("Client {} does not have a configuration. Will use default configuration", client);
            config = confService.getDefaultConfiguration();
        }
        return config;
    }

    protected final void publishEvent(AnalysisCompleteEvent event){
        if(eventPublisher != null){
            eventPublisher.publishEvent(event);
        }
    }
 
    
    private class AnalysisCompleteEventImpl implements AnalysisCompleteEvent {

        private final Client client;
        private final Conversation conversation;
        private final Date date;
        private final Analysis analysis;
        
        private AnalysisCompleteEventImpl(Client client, Conversation conversation, Date date, Analysis analysis){
            this.client = client;
            this.conversation = conversation;
            this.date = date;
            this.analysis = analysis;
        }
        
        @Override
        public Client getClient() {
            return client;
        }

        @Override
        public Conversation getConversation() {
            return conversation;
        }

        @Override
        public Date getDate() {
            return date;
        }

        @Override
        public Analysis getAnalysis() {
            return analysis;
        }
        
    }
    private static class AnalysisKey {
        
        private final Entry<ObjectId, ObjectId> entry;
        private final Date date;
        
        public AnalysisKey(Conversation c, Configuration conf) {
            assert c != null;
            assert conf != null;
            assert c.getId() != null;
            assert conf.getId() != null;
            assert conf.getClient() != null;
            assert c.getLastModified() != null;
            assert conf.getModified() != null;
            entry = new ImmutablePair<>(conf.getClient(), c.getId());
            date = c.getLastModified().after(conf.getModified()) ? c.getLastModified() : conf.getModified();
        }
        
        public Entry<ObjectId, ObjectId> getEntry() {
            return entry;
        }
        public Date getDate() {
            return date;
        }

        public ObjectId getClient() {
            return entry.getKey();
        }

        public ObjectId getConversation() {
            return entry.getValue();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + entry.hashCode();
            result = prime * result + date.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            AnalysisKey other = (AnalysisKey) obj;
            if(!entry.equals(other.entry)) {
                return false;
            }
            if(!date.equals(other.date)) {
                return false;
            }
            return true;
        }
        
        
        
        
    }
    
}
