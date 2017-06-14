/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.query.conversation;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.*;

import java.io.IOException;
import java.util.*;

/**
 */
public abstract class ConversationQueryBuilder extends QueryBuilder {

    private final String creatorName;
    protected final SolrCoreContainer solrServer;
    protected final SolrCoreDescriptor conversationCore;

    public ConversationQueryBuilder(String creatorName, SolrCoreContainer solrServer, SolrCoreDescriptor conversationCore, TemplateRegistry registry) {
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
    public boolean acceptTemplate(Template template) {
        return RELATED_CONVERSATION_TYPE.equals(template.getType()) && 
                template.getSlots().stream() //at least a single filled slot
                    .filter(s -> s.getRole().equals(ROLE_KEYWORD) || s.getRole().equals(ROLE_TERM))
                    .filter(s -> s.getTokenIndex() >= 0)
                    .findAny().isPresent();
    }

    @Override
    protected void doBuildQuery(Template template, Conversation conversation) {
        final Query query = buildQuery(template, conversation);
        if (query != null) {
            template.getQueries().add(query);
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
    public List<? extends Result> execute(Template intent, Conversation conversation) throws IOException {
        final QueryRequest solrRequest = buildSolrRequest(intent, conversation);
        if (solrRequest == null) {
            return Collections.emptyList();
        }

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final NamedList<Object> response = solrClient.request(solrRequest);
            final QueryResponse solrResponse = new QueryResponse(response, solrClient);

            final List<Result> results = new ArrayList<>();
            for (SolrDocument solrDocument : solrResponse.getResults()) {
                //get the answers /TODO hacky, should me refactored (at least ordered by rating)
                SolrQuery query = new SolrQuery("*:*");
                query.add("fq",String.format("conversation_id:\"%s\"",solrDocument.get("conversation_id")));
                query.add("fq",String.format("message_idx:[1 TO *]"));
                query.setFields("*","score");
                query.setSort("time", SolrQuery.ORDER.asc);
                query.setRows(3);

                QueryResponse answers = solrClient.query(query);

                results.add(toHassoResult(solrDocument, answers.getResults(), intent.getType()));
            }
            return results;
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    protected abstract ConversationResult toHassoResult(SolrDocument question, SolrDocumentList answersResults, String type);

    protected abstract QueryRequest buildSolrRequest(Template intent, Conversation conversation);

    protected abstract ConversationResult toHassoResult(SolrDocument solrDocument, String type);

    protected abstract Query buildQuery(Template intent, Conversation conversation);
}
