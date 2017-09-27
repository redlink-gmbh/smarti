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

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Query;
import io.redlink.smarti.model.SearchResult;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_OWNER;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.*;

/**
 */
public abstract class ConversationQueryBuilder extends QueryBuilder<ComponentConfiguration> {

    protected final SolrCoreContainer solrServer;
    protected final SolrCoreDescriptor conversationCore;

    public ConversationQueryBuilder(String creatorName, SolrCoreContainer solrServer, SolrCoreDescriptor conversationCore, TemplateRegistry registry) {
        super(ComponentConfiguration.class, registry);
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state = RELATED_CONVERSATION_TYPE.equals(template.getType()) &&
                template.getSlots().stream() //at least a single filled slot
                    .filter(s -> s.getRole().equals(ROLE_KEYWORD) || s.getRole().equals(ROLE_TERM))
                    .anyMatch(s -> s.getTokenIndex() >= 0);
        log.trace("{} does {}accept {}", this, state ? "" : "not ", template);
        return state;
    }

    @Override
    protected void doBuildQuery(ComponentConfiguration config, Template template, Conversation conversation) {
        final Query query = buildQuery(config, template, conversation);
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
    public SearchResult<? extends Result> execute(ComponentConfiguration conf, Template intent, Conversation conversation, MultiValueMap<String, String> queryParams) throws IOException {
        // TODO: use the queryParams
        final QueryRequest solrRequest = buildSolrRequest(conf, intent, conversation, queryParams);
        if (solrRequest == null) {
            return new SearchResult<>();
        }

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final NamedList<Object> response = solrClient.request(solrRequest);
            final QueryResponse solrResponse = new QueryResponse(response, solrClient);

            final List<Result> results = new ArrayList<>();
            for (SolrDocument solrDocument : solrResponse.getResults()) {
                //get the answers /TODO hacky, should me refactored (at least ordered by rating)
                SolrQuery query = new SolrQuery("*:*");
                query.add("fq",String.format("conversation_id:\"%s\"",solrDocument.get("conversation_id")));
                query.add("fq", "message_idx:[1 TO *]");
                query.setFields("*","score");
                query.setSort("time", SolrQuery.ORDER.asc);
                //query.setRows(3);

                QueryResponse answers = solrClient.query(query);

                results.add(toHassoResult(conf, solrDocument, answers.getResults(), intent.getType()));
            }
            return new SearchResult<>(results);
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public boolean validate(ComponentConfiguration configuration, Set<String> missing,
            Map<String, String> conflicting) {
        return true; //no config for now
    }
    
    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        return new ComponentConfiguration(); //this queryBuilder has no config params
    }

    protected abstract ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument question, SolrDocumentList answersResults, String type);

    protected abstract QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation, MultiValueMap<String, String> queryParams);

    protected abstract ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument solrDocument, String type);

    protected abstract Query buildQuery(ComponentConfiguration config, Template intent, Conversation conversation);
    
    /**
     * Adds a FilterQuery that ensures that only conversations with the same <code>owner</code> as
     * the current conversation are returned.
     * @param solrQuery the SolrQuery to add the FilterQuery
     * @param conversation the current conversation
     */
    protected final void addClientFilter(final SolrQuery solrQuery, Conversation conversation) {
        solrQuery.addFilterQuery(FIELD_OWNER + ':' + conversation.getOwner().toHexString());
    }


}
