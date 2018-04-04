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

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.SearchResult;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.*;
import static org.apache.commons.lang3.math.NumberUtils.toInt;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 09.02.17.
 */
@Component
public class ConversationMltQueryBuilder extends ConversationQueryBuilder {

    public static final String CREATOR_NAME = "query_related_mlt";

    @Autowired
    public ConversationMltQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore, 
            TemplateRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state = RELATED_CONVERSATION_TYPE.equals(template.getType());
        log.trace("{} does {} accept {}", this, state ? "" : "not ", template);
        return state;
    }
    
    //This query builder support execution if the solr core is up and running
    @Override
    public final boolean isResultSupported() {
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
    public SearchResult<? extends Result> execute(ComponentConfiguration conf, Template template, Conversation conversation, Analysis analysis, MultiValueMap<String, String> queryParams) throws IOException {
        // read default page-size from builder-configuration
        int pageSize = conf.getConfiguration(CONFIG_KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        // if present, a queryParam 'rows' takes precedence.
        pageSize = toInt(queryParams.getFirst("rows"), pageSize);
        long offset = toInt(queryParams.getFirst("start"), 0);


        final QueryRequest solrRequest = buildSolrRequest(conf, template, conversation, analysis, offset, pageSize, queryParams);
        if (solrRequest == null) {
            return new SearchResult<ConversationResult>(pageSize);
        }

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final NamedList<Object> response = solrClient.request(solrRequest);
            final QueryResponse solrResponse = new QueryResponse(response, solrClient);
            final SolrDocumentList solrResults = solrResponse.getResults();

            final List<ConversationResult> results = new ArrayList<>();
            for (SolrDocument solrDocument : solrResults) {
                //get the answers /TODO hacky, should me refactored (at least ordered by rating)
                SolrQuery query = new SolrQuery("*:*");
                query.add("fq",String.format("%s:\"%s\"",FIELD_CONVERSATION_ID,solrDocument.get(FIELD_CONVERSATION_ID)));
                query.add("fq", FIELD_MESSAGE_IDXS + ":[1 TO *]");
                query.setFields("*","score");
                query.setSort("time", SolrQuery.ORDER.asc);
                //query.setRows(3);

                QueryResponse answers = solrClient.query(query);

                results.add(toConverationResult(conf, solrDocument, answers.getResults(), template.getType()));
            }
            return new SearchResult<>(solrResults.getNumFound(), solrResults.getStart(), pageSize, results);
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    private ConversationResult toConversationResult(ComponentConfiguration conf, SolrDocument solrDocument, String type) {
        final ConversationResult conversationResult = new ConversationResult(getCreatorName(conf));

        conversationResult.setScore(Double.parseDouble(String.valueOf(solrDocument.getFieldValue("score"))));

        conversationResult.setContent(String.valueOf(solrDocument.getFirstValue(FIELD_MESSAGE)));
        conversationResult.setReplySuggestion(conversationResult.getContent());

        conversationResult.setConversationId(String.valueOf(solrDocument.getFieldValue(FIELD_CONVERSATION_ID)));
        conversationResult.setMessageId(String.valueOf(solrDocument.getFirstValue(FIELD_MESSAGE_IDS)));
        conversationResult.setMessageIdx(Integer.parseInt(String.valueOf(solrDocument.getFirstValue(FIELD_MESSAGE_IDXS))));

        conversationResult.setVotes(Integer.parseInt(String.valueOf(solrDocument.getFieldValue(FIELD_VOTE))));

        conversationResult.setTimestamp((Date) solrDocument.getFieldValue(FIELD_TIME));
        conversationResult.setUserName((String) solrDocument.getFieldValue(FIELD_USER_NAME));

        return conversationResult;
    }

    private ConversationResult toConverationResult(ComponentConfiguration conf, SolrDocument question, SolrDocumentList answers, String type) {
        ConversationResult result = toConversationResult(conf, question, type);
        for(SolrDocument answer : answers) {
            result.addAnswer(toConversationResult(conf, answer,type));
        }
        return result;
    }

    @Override
    protected ConversationMltQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        if (conversation.getMessages().isEmpty()) return null;

        //The context is the content of relevant messages (see #getContextStart(..) for more information
        String context = conversation.getMessages().subList(
                ConversationContextUtils.getContextStart(conversation.getMessages(),
                    MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE), 
                conversation.getMessages().size()).stream()
            .sequential()
            .map(Message::getContent)
            .reduce(null, (s, e) -> {
                if (s == null) return e;
                return s + "\n\n" + e;
            });
        
        if(StringUtils.isBlank(context)){
            return null; //no content in the conversation to search for releated!
        }

        final String displayTitle = StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName());

        return new ConversationMltQuery(getCreatorName(conf))
                .setInlineResultSupport(isResultSupported())
                .setDisplayTitle(displayTitle)
                .setConfidence(.55f)
                .setState(State.Suggested)
                .setContent(context.toString());
    }
    
    private QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis, long offset, int pageSize, MultiValueMap<String, String> queryParams) {
        final ConversationMltQuery mltQuery = buildQuery(conf, intent, conversation, analysis);
        if (mltQuery == null) {
            return null;
        }

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("*").addField("score");
        solrQuery.addFilterQuery(String.format("%s:%s", FIELD_TYPE, TYPE_MESSAGE));
        solrQuery.addFilterQuery(String.format("%s:0",FIELD_MESSAGE_IDXS));
        solrQuery.addSort("score", SolrQuery.ORDER.desc).addSort(FIELD_VOTE, SolrQuery.ORDER.desc);

        // #39 - paging
        solrQuery.setStart((int) offset);
        solrQuery.setRows(pageSize);

        //since #46 the client field is used to filter for the current user
        addClientFilter(solrQuery, conversation);
        
        //since #191
        if(conf.getConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY)){
            addCompletedFilter(solrQuery);
        }

        addPropertyFilters(solrQuery, conversation, conf);

        return new ConversationMltRequest(solrQuery, mltQuery.getContent());

    }


}
