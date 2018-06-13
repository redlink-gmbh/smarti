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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.AtomicDouble;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        int contextStart = ConversationContextUtils.getContextStart(conversation.getMessages(),
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE);

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
            query.addFilter(getCompletedFilter());
        }
        if(conf.getConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT)){
            query.addFilter(new Filter("filter.excludeCurrentConversation", 
                    '-' + FIELD_CONVERSATION_ID + ':' + conversation.getId().toHexString()));
        }
        //add config specific filter queries
        query.getFilters().addAll(getPropertyFilters(conversation, conf));
        
        try {
            query.setSimilarityQuery(buildContextQuery(conversation,conf));
            log.trace("similarityQuery: {}", query.getSimilarityQuery());
        } catch (IOException | SolrServerException e) {
            if(log.isDebugEnabled()){
                log.warn("Unable to build SimilarityQuery for {}",conversation, e);
            } else {
                log.warn("Unable to build SimilarityQuery for {} ({} - {})", conversation, e.getClass().getSimpleName(), e.getMessage());
            }
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
    
    private String buildContextQuery(Conversation conv, ComponentConfiguration conf) throws IOException, SolrServerException{
        String context = conv.getMessages().subList(
                ConversationContextUtils.getContextStart(conv.getMessages(), 
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE),
                conv.getMessages().size()).stream()
            .map(Message::getContent)
            .reduce(null, (s, e) -> {
                if (s == null) return e;
                return s + "\n\n" + e;
            });
        log.trace("SimilarityContext: {}", context);
        if(StringUtils.isBlank(context)){ //fix for #2258
            return ""; //for an empty context use an empty query
        }

        //we make a MLT query to get the interesting terms
        final SolrQuery solrQuery = new SolrQuery();
        //search interesting terms in the conversations (as those do not have overlapping
        //contexts as messages)
        solrQuery.addFilterQuery(String.format("%s:%s", FIELD_TYPE, TYPE_CONVERSATION));
        //respect client filters
        addClientFilter(solrQuery, conv);
        //and also property related filters
        addPropertyFilters(solrQuery, conv, conf);
        //and completed query
        if(conf.getConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY)){
            addCompletedFilter(solrQuery);
        }
        //we do not want any results
        solrQuery.setRows(0);
        //we search for interesting terms in the MLT Context field
        solrQuery.add(MoreLikeThisParams.SIMILARITY_FIELDS, FIELD_MLT_CONTEXT);
        solrQuery.add(MoreLikeThisParams.BOOST,String.valueOf(true));
        solrQuery.add(MoreLikeThisParams.INTERESTING_TERMS,"details");
        solrQuery.add(MoreLikeThisParams.MAX_QUERY_TERMS, String.valueOf(10));
        solrQuery.add(MoreLikeThisParams.MIN_WORD_LEN, String.valueOf(3));
        
        log.trace("InterestingTerms QueryParams: {}", solrQuery);
        
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            NamedList<Object> response = solrClient.request(new ConversationMltRequest(solrQuery, context));
            NamedList<Object> interestingTermList = (NamedList<Object>)response.get("interestingTerms");
            if(interestingTermList == null || interestingTermList.size() < 1) { //no interesting terms
                return null;
            } else {
                //Do make it easier to combine context params with other ensure that the maximum boost is 1.0
                AtomicDouble norm = new AtomicDouble(-1);
                return StreamSupport.stream(interestingTermList.spliterator(), false)
                    .sorted((a,b) -> Double.compare(((Number)b.getValue()).doubleValue(), ((Number)a.getValue()).doubleValue()))
                    .map(e -> {
                        //NOTE: we need to query escape the value of the term as the returned
                        // interesting terms are not!
                        final String term;
                        int fieldSepIdx = e.getKey().indexOf(':'); //check if their 
                        if(fieldSepIdx >= 0){
                            term = e.getKey().substring(0, fieldSepIdx + 1) + 
                                    ClientUtils.escapeQueryChars(e.getKey().substring(fieldSepIdx + 1));
                        } else {
                            term = ClientUtils.escapeQueryChars(e.getKey());
                        }
                        if(norm.doubleValue() < 0) {
                            norm.set(1d/((Number)e.getValue()).doubleValue());
                            return term;
                        } else {
                            return term + '^' + (((Number)e.getValue()).floatValue() * norm.floatValue());
                        }
                    })
                    .collect(Collectors.joining(" OR "));
            }
        } catch (SolrException solrEx) { //related to #2258 - make code more robust to unexpected errors
            if(log.isDebugEnabled()){
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}'", 
                        solrQuery, context, solrEx);
            } else {
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}' ({} - {})", 
                        solrQuery, context, solrEx.getClass().getSimpleName(), solrEx.getMessage());
            }
            return ""; //return an empty context query as fallback
        }
    }

    
}
