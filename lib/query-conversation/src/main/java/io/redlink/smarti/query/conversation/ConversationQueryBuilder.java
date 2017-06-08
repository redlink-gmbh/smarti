/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.NamedList;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Intend;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.IntendRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

import java.io.IOException;
import java.util.*;

/**
 */
public abstract class ConversationQueryBuilder extends QueryBuilder {

    private static final Set<MessageTopic> ACCEPTED_TYPES = EnumSet.of(MessageTopic.ApplicationHelp, MessageTopic.Sonstiges);
    private final String creatorName;
    protected final SolrCoreContainer solrServer;
    protected final SolrCoreDescriptor conversationCore;

    public ConversationQueryBuilder(String creatorName, SolrCoreContainer solrServer, SolrCoreDescriptor conversationCore, IntendRegistry registry) {
        super(registry);
        this.creatorName = creatorName;
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
    }

    @Override
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public boolean acceptTemplate(Intend intend) {
        return ACCEPTED_TYPES.contains(intend.getType());
    }

    @Override
    protected void doBuildQuery(Intend intend, Conversation conversation) {
        final Query query = buildQuery(intend, conversation);
        if (query != null) {
            intend.getQueries().add(query);
        }
    }

    @Override
    public boolean isResultSupported() {
        if(solrServer != null && conversationCore != null){
            try (SolrClient solr = solrServer.getSolrClient(conversationCore)){
                return solr.ping().getStatus() == 0;
            } catch (SolrServerException | IOException e) {
                log.warn("Results currently not supported because ping to {} failed ({} - {})", conversationCore, e.getClass().getSimpleName(), e.getMessage());
                log.debug("STACKTRACE: ", e);
            }
        }
        return false;
    }

    @Override
    public List<? extends Result> execute(Intend template, Conversation conversation) throws IOException {
        final QueryRequest solrRequest = buildSolrRequest(template, conversation);
        if (solrRequest == null) {
            return Collections.emptyList();
        }

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final NamedList<Object> response = solrClient.request(solrRequest);
            final QueryResponse solrResponse = new QueryResponse(response, solrClient);

            final List<Result> results = new ArrayList<>();
            for (SolrDocument solrDocument : solrResponse.getResults()) {
                results.add(toHassoResult(solrDocument, template.getType()));
            }
            return results;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    protected abstract QueryRequest buildSolrRequest(Intend intend, Conversation conversation);

    protected abstract ConversationResult toHassoResult(SolrDocument solrDocument, MessageTopic type);

    protected abstract Query buildQuery(Intend intend, Conversation conversation);
}
