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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;
import static io.redlink.smarti.query.conversation.ConversationSearchService.DEFAULT_CONTEXT_AFTER;
import static io.redlink.smarti.query.conversation.ConversationSearchService.DEFAULT_CONTEXT_BEFORE;
import static io.redlink.smarti.query.conversation.ConversationSearchService.PARAM_CONTEXT_AFTER;
import static io.redlink.smarti.query.conversation.ConversationSearchService.PARAM_CONTEXT_BEFORE;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.*;

/**
 */
@Component
public class ConversationSearchQueryBuilder extends ConversationQueryBuilder {


    public static final String CONFIG_KEY_DEFAULTS = "defaults";
    
    public static final String CREATOR_NAME = "query_related_search";

    @Autowired
    public ConversationSearchQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore,
            TemplateRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

//    @Override
//    protected QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis, long offset, int pageSize, MultiValueMap<String, String> queryParams) {
//        final ConversationSearchQuery searchQuery = buildQuery(conf, intent, conversation, analysis);
//        if (searchQuery == null) {
//            return null;
//        }
//
//        // TODO: build the real request.
//        final SolrQuery solrQuery = new SolrQuery(searchQuery.getKeyword().stream().reduce((s, s2) -> s + " OR " + s2).orElse("*:*"));
//        solrQuery.addField("*").addField("score");
//        solrQuery.set(CommonParams.DF, "text");
//        solrQuery.addFilterQuery(String.format("%s:%s",FIELD_TYPE, TYPE_MESSAGE));
//        solrQuery.addSort("score", SolrQuery.ORDER.desc).addSort("vote", SolrQuery.ORDER.desc);
//
//        // #39 - paging
//        solrQuery.setStart((int) offset);
//        solrQuery.setRows(pageSize);
//
//        //since #46 the client field is used to filter for the current user
//        addClientFilter(solrQuery, conversation);
//        
//        //since #191
//        if(conf.getConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY)){
//            addCompletedFilter(solrQuery);
//        }
//        
//        addPropertyFilters(solrQuery, conversation, conf);
//        
//        return new QueryRequest(solrQuery);
//    }

//    @Override
//    protected ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument solrDocument, String type) {
//        final ConversationResult hassoResult = new ConversationResult(getCreatorName(conf));
//        hassoResult.setScore(Double.parseDouble(String.valueOf(solrDocument.getFieldValue("score"))));
//        hassoResult.setContent(String.valueOf(solrDocument.getFirstValue("message")));
//        hassoResult.setReplySuggestion(hassoResult.getContent());
//        hassoResult.setConversationId(String.valueOf(solrDocument.getFieldValue("conversation_id")));
//        hassoResult.setMessageIdx(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("message_idx"))));
//        hassoResult.setVotes(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("vote"))));
//        return hassoResult;
//    }

//    @Override
//    protected ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument question, SolrDocumentList answers, String type) {
//        ConversationResult result = toHassoResult(conf, question, type);
//        for(SolrDocument answer : answers) {
//            result.addAnswer(toHassoResult(conf, answer,type));
//        }
//        return result;
//    }

    @Override
    protected ConversationSearchQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        List<Token> keywords = getTokens(ROLE_KEYWORD, intent, analysis);
        List<Token> terms = getTokens(ROLE_TERM, intent, analysis);
        int contextStart = getContextStart(conversation.getMessages());

        final ConversationSearchQuery query = new ConversationSearchQuery(getCreatorName(conf));

        final String displayTitle = StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName());

        query.setInlineResultSupport(isResultSupported())
                .setState(State.Suggested)
                .setConfidence(.6f)
                .setDisplayTitle(displayTitle);
        
        //relative uri to the conversation search service
        query.setUrl("/conversation/search");
        
        keywords.stream()
                .filter(t -> t.getMessageIdx() >= contextStart)
                .map(Token::getValue)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(() -> query.getKeywords()));
        
        terms.stream()
                .filter(t -> t.getMessageIdx() >= contextStart)
                .map(Token::getValue)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(() -> query.getTerms()));
        
        //apply the defaults from the configuration
        Object value = conf.getConfiguration(CONFIG_KEY_DEFAULTS);
        if(value instanceof Map){
            query.getDefaults().putAll((Map<String,Object>)value);;
        } else if(value instanceof Collection){
            ((Collection<Object>)value).stream()
                .filter(v -> v instanceof Map)
                .map(v -> Map.class.cast(v))
                .filter(m -> m.containsKey("key") && m.containsKey("value"))
                .forEach(m -> query.getDefaults().put(String.valueOf(m.get("key")), m.get("value")));
        }
        //apply the ROE
        //NOTE: do not override if row is not set but rows is set as default 
        if(conf.isConfiguration(CONFIG_KEY_PAGE_SIZE) || !query.getDefaults().containsKey("rows")){
            query.getDefaults().put("rows", conf.getConfiguration(CONFIG_KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE));
        }
        if(!query.getDefaults().containsKey("fl")){
            query.getDefaults().put("fl", "id,message_id,meta_channel_id,user_id,time,message,type");
        }
        if(conf.getConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY)){
            query.addFilterQuery(FIELD_COMPLETED + ":true");
        }
        if(conf.getConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT)){
            query.addFilterQuery('-' + FIELD_CONVERSATION_ID + ':' + conversation.getId().toHexString());
        }
        //add config specific filter queries
        SolrQuery sq = new SolrQuery();
        addPropertyFilters(sq, conversation, conf);
        if(sq.getFilterQueries() != null){
            Arrays.stream(sq.getFilterQueries()).forEach(query::addFilterQuery);
        }
        return query;
    }
    
    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        ComponentConfiguration cc = super.getDefaultConfiguration();
        if(cc == null){
            cc = new ComponentConfiguration();
        }
        cc.setConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT);
        //add defaults
        Map<String,Object> defaults = new HashMap<>();
        defaults.put(PARAM_CONTEXT_BEFORE, DEFAULT_CONTEXT_BEFORE);
        defaults.put(PARAM_CONTEXT_AFTER, DEFAULT_CONTEXT_AFTER);
        cc.setConfiguration(CONFIG_KEY_DEFAULTS, defaults);
// alternative format also supported by this implementation
//        cc.setConfiguration(CONFIG_KEY_DEFAULTS, defaults.entrySet().stream()
//                .map(e -> {
//                    Map<String,Object> map = new HashMap<>(2);
//                    map.put("key", e.getKey());
//                    map.put("value", e.getValue());
//                    return map;
//                })
//                .collect(Collectors.toList()));
        return cc;
    }
    
}
