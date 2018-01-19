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

import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.stream.Collectors;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_TYPE;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.TYPE_MESSAGE;

/**
 */
@Component
public class ConversationSearchQueryBuilder extends ConversationQueryBuilder {

    public static final String CREATOR_NAME = "query_related_search";

    @Autowired
    public ConversationSearchQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore,
            TemplateRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

    @Override
    protected QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis, long offset, int pageSize, MultiValueMap<String, String> queryParams) {
        final ConversationSearchQuery searchQuery = buildQuery(conf, intent, conversation, analysis);
        if (searchQuery == null) {
            return null;
        }

        // TODO: build the real request.
        final SolrQuery solrQuery = new SolrQuery(searchQuery.getKeyword().stream().reduce((s, s2) -> s + " OR " + s2).orElse("*:*"));
        solrQuery.addField("*").addField("score");
        solrQuery.set(CommonParams.DF, "text");
        solrQuery.addFilterQuery(String.format("%s:%s",FIELD_TYPE, TYPE_MESSAGE));
        solrQuery.addSort("score", SolrQuery.ORDER.desc).addSort("vote", SolrQuery.ORDER.desc);

        // #39 - paging
        solrQuery.setStart((int) offset);
        solrQuery.setRows(pageSize);

        //since #46 the client field is used to filter for the current user
        addClientFilter(solrQuery, conversation);

        addPropertyFilters(solrQuery, conversation, conf);
        
        return new QueryRequest(solrQuery);
    }

    @Override
    protected ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument solrDocument, String type) {
        final ConversationResult hassoResult = new ConversationResult(getCreatorName(conf));
        hassoResult.setScore(Double.parseDouble(String.valueOf(solrDocument.getFieldValue("score"))));
        hassoResult.setContent(String.valueOf(solrDocument.getFirstValue("message")));
        hassoResult.setReplySuggestion(hassoResult.getContent());
        hassoResult.setConversationId(String.valueOf(solrDocument.getFieldValue("conversation_id")));
        hassoResult.setMessageIdx(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("message_idx"))));
        hassoResult.setVotes(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("vote"))));
        return hassoResult;
    }

    @Override
    protected ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument question, SolrDocumentList answers, String type) {
        ConversationResult result = toHassoResult(conf, question, type);
        for(SolrDocument answer : answers) {
            result.addAnswer(toHassoResult(conf, answer,type));
        }
        return result;
    }

    @Override
    protected ConversationSearchQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        final List<Token> keywords = getTokens("keyword", intent, analysis);
        if (keywords == null || keywords.isEmpty()) return null;

        // TODO: Build the real query.
        final ConversationSearchQuery query = new ConversationSearchQuery(getCreatorName(conf));
        final List<String> strs = keywords.stream()
                .map(Token::getValue)
                .map(String::valueOf)
                .collect(Collectors.toList());
        String displayTitle = String.format("%s zum Thema %s",
                StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName()),        strs);
        if (StringUtils.isNotBlank(conversation.getContext().getDomain())) {
            displayTitle += " (" + conversation.getContext().getDomain() + ")";
        }

        query.setInlineResultSupport(isResultSupported())
                .setState(State.Suggested)
                .setConfidence(.6f)
                .setDisplayTitle(displayTitle);

        query.setKeywords(strs);

        return query;
    }
}
