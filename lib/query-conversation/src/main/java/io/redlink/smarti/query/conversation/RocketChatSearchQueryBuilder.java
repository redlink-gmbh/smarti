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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
public class RocketChatSearchQueryBuilder extends ConversationQueryBuilder {


    public static final String CONFIG_KEY_PAYLOAD = "payload";
    
    public static final String PARAM_PAYLOAD_ROWS = "rows";
    public static final Integer DEFAULT_PLAYLOAD_ROWS = 10;
    
    public static final String CONFIG_KEY_EXCLUDE_CURRENT = "excludeCurrentChannel";
    public static final Boolean DEFAULT_EXCLUDE_CURRENT = Boolean.FALSE;
    
    public static final String CREATOR_NAME = "query_rocketchat_search";

    @Autowired
    public RocketChatSearchQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore,
            TemplateRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

    @Override
    protected RocketChatSearchQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        List<Token> keywords = getTokens(ROLE_KEYWORD, intent, analysis);
        List<Token> terms = getTokens(ROLE_TERM, intent, analysis);
        int contextStart = ConversationContextUtils.getContextStart(conversation.getMessages(),
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE);

        final RocketChatSearchQuery query = new RocketChatSearchQuery(getCreatorName(conf));

        final String displayTitle = StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName());

        query.setInlineResultSupport(isResultSupported())
                .setState(State.Suggested)
                .setConfidence(.6f)
                .setDisplayTitle(displayTitle);
        
        query.setUrl(null);
        
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
        
        //apply the payload from the configuration
        Object value = conf.getConfiguration(CONFIG_KEY_PAYLOAD);
        if(value instanceof Map){
            query.getPayload().putAll((Map<String,Object>)value);;
        } else if(value instanceof Collection){
            ((Collection<Object>)value).stream()
                .filter(v -> v instanceof Map)
                .map(v -> Map.class.cast(v))
                .filter(m -> m.containsKey("key") && m.containsKey("value"))
                .forEach(m -> query.getPayload().put(String.valueOf(m.get("key")), m.get("value")));
        }
        //apply the ROE
        //NOTE: do not override if row is not set but rows is set as default 
        if(conf.isConfiguration(CONFIG_KEY_PAGE_SIZE) || !query.getPayload().containsKey("rows")){
            query.getPayload().put("rows", conf.getConfiguration(CONFIG_KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE));
        }
        //copy defined parameters 
        query.setParam(CONFIG_KEY_EXCLUDE_CURRENT, conf.getConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT));

        //TODO: maybe we want Filter support as for other releated conversations Query Builder
        //query.getFilters().addAll(getPropertyFilters(conversation, conf));
        
        try {
            buildContextQuery(conversation, conf, query);
        } catch (IOException | SolrServerException e) {
            if(log.isDebugEnabled()){
                log.warn("Unable to build ContextQuery for {}",conversation, e);
            } else {
                log.warn("Unable to build ContextQuery for {} ({} - {})", conversation, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return query;
    }
    
    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        //ComponentConfiguration cc = super.getDefaultConfiguration();
        //if(cc == null){
        //    cc = new ComponentConfiguration();
        //}
        ComponentConfiguration cc = new ComponentConfiguration();
        cc.setConfiguration(CONFIG_KEY_EXCLUDE_CURRENT, DEFAULT_EXCLUDE_CURRENT);
        //add defaults
        Map<String,Object> payload = new HashMap<>();
        payload.put(PARAM_PAYLOAD_ROWS, DEFAULT_PLAYLOAD_ROWS);
        cc.setConfiguration(CONFIG_KEY_PAYLOAD, payload);
        return cc;
    }
    
    private void buildContextQuery(Conversation conv, ComponentConfiguration conf, RocketChatSearchQuery query) throws IOException, SolrServerException{
        String context = conv.getMessages().subList(
                ConversationContextUtils.getContextStart(conv.getMessages(), 
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE),
                conv.getMessages().size()).stream()
            .filter(m -> !MapUtils.getBoolean(m.getMetadata(), Message.Metadata.SKIP_ANALYSIS, false))
            .map(Message::getContent)
            .reduce(null, (s, e) -> {
                if (s == null) return e;
                return s + "\n\n" + e;
            });
        log.trace("SimilarityContext: {}", context);
        if(StringUtils.isBlank(context)){ //fix for #2258
            return; //for an empty context use an empty query
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
            if(interestingTermList != null && interestingTermList.size() > 0) { //interesting terms present
                //Do make it easier to combine context params with other ensure that the maximum boost is 1.0
                AtomicDouble norm = new AtomicDouble(-1);
                StreamSupport.stream(interestingTermList.spliterator(), false)
                    .sorted((a,b) -> Double.compare(((Number)b.getValue()).doubleValue(), ((Number)a.getValue()).doubleValue()))
                    .forEach(e -> {
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
                        //TODO: Try to map the term with Terms and Keywords extracted from the conversation
                        final float relevance;
                        if(norm.doubleValue() < 0) {
                            norm.set(1d/((Number)e.getValue()).doubleValue());
                            relevance = 1f;
                        } else {
                            relevance = (((Number)e.getValue()).floatValue() * norm.floatValue());
                        }
                        query.addContextQueryTerm(term, relevance);
                    });
            }
        } catch (SolrException solrEx) { //related to #2258 - make code more robust to unexpected errors
            if(log.isDebugEnabled()){
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}'", 
                        solrQuery, context, solrEx);
            } else {
                log.warn("Unable to build ContextQuery using solrQuery: {} and context: '{}' ({} - {})", 
                        solrQuery, context, solrEx.getClass().getSimpleName(), solrEx.getMessage());
            }
        }
    }

    
}
