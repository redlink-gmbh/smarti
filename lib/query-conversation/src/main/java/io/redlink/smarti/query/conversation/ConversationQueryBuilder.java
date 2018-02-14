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
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.*;

/**
 */
public abstract class ConversationQueryBuilder extends QueryBuilder<ComponentConfiguration> {

    //#192: We target more as 100 chars are context
    protected static final int MIN_CONTEXT_LENGTH = 100;
    protected static final int CONTEXT_LENGTH = 300;
    //#192: Include at least the last two messages 
    protected static final int MIN_INCL_MSGS = 2;
    protected static final int MAX_INCL_MSGS = 10;
    //#192: Include at least all messages of the last 5 minutes
    protected static final long MIN_AGE = TimeUnit.MINUTES.toMillis(5);
    //#192: Include at least all messages of the last 5 minutes
    protected static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    
    public static final String CONFIG_KEY_PAGE_SIZE = "pageSize";
    public static final int DEFAULT_PAGE_SIZE = 3;

    public static final String CONFIG_KEY_FILTER = "filter";
    /**
     * Option that allows to configure the ConversationQueryBuilder to suggest only completed conversations
     */ //since #91
    public static final String CONFIG_KEY_COMPLETED_ONLY = "completedOnly";
    /**
     * {@link #CONFIG_KEY_COMPLETED_ONLY} is deactivated by default
     */
    public static final boolean DEFAULT_COMPLETED_ONLY = false;

    /**
     * If the current conversation should be excluded from related conversation results
     */
    public static final String CONFIG_KEY_EXCLUDE_CURRENT = "exclCurrentConv";
    
    public static final boolean DEFAULT_EXCLUDE_CURRENT = true;
    
    
    protected final SolrCoreContainer solrServer;
    protected final SolrCoreDescriptor conversationCore;

    public ConversationQueryBuilder(String creatorName, SolrCoreContainer solrServer, SolrCoreDescriptor conversationCore, TemplateRegistry registry) {
        super(ComponentConfiguration.class, registry);
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
    }

    @Override
    public boolean acceptTemplate(Template template) {
        boolean state = RELATED_CONVERSATION_TYPE.equals(template.getType()); // &&
        //with #200 queries should be build even if no slot is set
//                template.getSlots().stream() //at least a single filled slot
//                    .filter(s -> s.getRole().equals(ROLE_KEYWORD) || s.getRole().equals(ROLE_TERM))
//                    .anyMatch(s -> s.getTokenIndex() >= 0);
        log.trace("{} does {} accept {}", this, state ? "" : "not ", template);
        return state;
    }

    @Override
    protected void doBuildQuery(ComponentConfiguration config, Template template, Conversation conversation, Analysis analysis) {
        final Query query = buildQuery(config, template, conversation, analysis);
        if (query != null) {
            template.getQueries().add(query);
        }
    }


    protected int getContextStart(List<Message> messages){
        if(messages.isEmpty()){
            return 0;
        }
        int inclMsgs = 0;
        Date contextDate = null;
        int contextSize = 0;
        for(ListIterator<Message> it = messages.listIterator(messages.size()); 
                it.hasPrevious();){
            int index = it.previousIndex();
            Message msg = it.previous();
            if(contextDate == null){
                contextDate = msg.getTime();
            }
            if(contextSize < MIN_CONTEXT_LENGTH || //force inclusion
                    inclMsgs < MIN_INCL_MSGS || 
                    msg.getTime().getTime() > contextDate.getTime() - MIN_AGE){
                contextSize = contextSize + msg.getContent().length();
            } else if(contextSize < CONTEXT_LENGTH && //allow include if more context is allowed
                    inclMsgs < MAX_INCL_MSGS && 
                    msg.getTime().getTime() > contextDate.getTime() - MAX_AGE){
                contextSize = contextSize + msg.getContent().length();
            } else {
                return index; //we have enough content ... ignore previous messages
            }
        }
        return 0;
    }
    
    @Override
    public boolean validate(ComponentConfiguration configuration, Set<String> missing,
            Map<String, String> conflicting) {
        return true; //no config for now
    }
    
    @Override
    public ComponentConfiguration getDefaultConfiguration() {
        final ComponentConfiguration defaultConfig = new ComponentConfiguration();

        // #39 - make default page-size configurable
        defaultConfig.setConfiguration(CONFIG_KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        // with #87 we restrict results to the same support-area
        defaultConfig.setConfiguration(CONFIG_KEY_FILTER, Collections.singletonList(ConversationMeta.PROP_SUPPORT_AREA));
        //#191 support none completed conversations
        defaultConfig.setConfiguration(CONFIG_KEY_COMPLETED_ONLY, DEFAULT_COMPLETED_ONLY);
        
        return defaultConfig;
    }

    //protected abstract ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument question, SolrDocumentList answersResults, String type);

    //protected abstract ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument solrDocument, String type);

    protected abstract Query buildQuery(ComponentConfiguration config, Template intent, Conversation conversation, Analysis analysis);
    
    /**
     * Adds a FilterQuery that ensures that only conversations with the same <code>owner</code> as
     * the current conversation are returned.
     * @param solrQuery the SolrQuery to add the FilterQuery
     * @param conversation the current conversation
     */
    protected final void addClientFilter(final SolrQuery solrQuery, Conversation conversation) {
        solrQuery.addFilterQuery(FIELD_OWNER + ':' + conversation.getOwner().toHexString());
    }

    protected final void addCompletedFilter(final SolrQuery solrQuery){
        solrQuery.addFilterQuery(FIELD_COMPLETED + ":true");
    }
    
    protected void addPropertyFilters(SolrQuery solrQuery, Conversation conversation, ComponentConfiguration conf) {
        final List<String> filters = conf.getConfiguration(CONFIG_KEY_FILTER, Collections.emptyList());
        filters.forEach(f -> addPropertyFilter(solrQuery, f, conversation.getMeta().getProperty(f)));
    }
    /**
     * Adds a filter based on a {@link ConversationMeta#getProperty(String)}
     * @param solrQuery the Solr query to add the FilterQuery
     * @param fieldName the name of the field
     * @param fieldValues the field values
     */
    protected void addPropertyFilter(SolrQuery solrQuery, String fieldName, List<String> fieldValues) {
        if (fieldValues == null || !fieldValues.stream().anyMatch(StringUtils::isNotBlank)) {
            solrQuery.addFilterQuery("-" + getMetaField(fieldName) + ":*");
        } else {
            fieldValues.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(ClientUtils::escapeQueryChars)
                    .reduce((a, b) -> a + " OR " + b)
                    .ifPresent(filterVal ->
                            solrQuery.addFilterQuery(getMetaField(fieldName) + ":(" + filterVal + ")")
                    );
        }
    }
}
