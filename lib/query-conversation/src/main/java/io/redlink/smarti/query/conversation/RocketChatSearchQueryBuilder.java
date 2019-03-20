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

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_MLT_CONTEXT;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_TYPE;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.TYPE_CONVERSATION;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.ROLE_KEYWORD;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.ROLE_TERM;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.redlink.smarti.lib.solr.iterms.SolrInterestingTermsUtils;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.User;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

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
        
        query.setUsers(conversation.getMessages().subList(contextStart, conversation.getMessages().size()).stream()
                .map(Message::getUser).filter(Objects::nonNull)
                .map(User::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        
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
        int cxtStart = ConversationContextUtils.getContextStart(conv.getMessages(), 
                MIN_CONTEXT_LENGTH, CONTEXT_LENGTH, MIN_INCL_MSGS, MAX_INCL_MSGS, MIN_AGE, MAX_AGE);
        List<Message> ctxMsgs = conv.getMessages().subList(cxtStart, conv.getMessages().size());
        //add the context messages to Ids of the Messages
        ctxMsgs.forEach(msg -> query.addContextMsg(msg.getId()));
        String context = ctxMsgs.stream()
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
        
        //Init the SolrParams for the MLT query
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
        solrQuery.addMoreLikeThisField(FIELD_MLT_CONTEXT);
        solrQuery.setMoreLikeThisMaxQueryTerms(10);
        solrQuery.setMoreLikeThisMinWordLen(3);
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
           SolrInterestingTermsUtils.extractInterestingTerms(solrClient, solrQuery, context).forEach(cw -> {
                query.addContextQueryTerm(cw.getWord(), cw.getRelevance());
            });
        } catch (SolrException solrEx) { //related to #258 - make code more robust to unexpected errors
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
