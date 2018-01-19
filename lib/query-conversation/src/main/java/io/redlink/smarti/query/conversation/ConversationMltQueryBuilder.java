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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.Date;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.RELATED_CONVERSATION_TYPE;

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

    @Override
    protected ConversationResult toHassoResult(ComponentConfiguration conf, SolrDocument solrDocument, String type) {
        final ConversationResult hassoResult = new ConversationResult(getCreatorName(conf));

        hassoResult.setScore(Double.parseDouble(String.valueOf(solrDocument.getFieldValue("score"))));

        hassoResult.setContent(String.valueOf(solrDocument.getFirstValue(FIELD_MESSAGE)));
        hassoResult.setReplySuggestion(hassoResult.getContent());

        hassoResult.setConversationId(String.valueOf(solrDocument.getFieldValue(FIELD_CONVERSATION_ID)));
        hassoResult.setMessageId(String.valueOf(solrDocument.getFieldValue(FIELD_MESSAGE_ID)));
        hassoResult.setMessageIdx(Integer.parseInt(String.valueOf(solrDocument.getFieldValue(FIELD_MESSAGE_IDX))));

        hassoResult.setVotes(Integer.parseInt(String.valueOf(solrDocument.getFieldValue(FIELD_VOTE))));

        hassoResult.setTimestamp((Date) solrDocument.getFieldValue(FIELD_TIME));
        hassoResult.setUserName((String) solrDocument.getFieldValue(FIELD_USER_NAME));

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
    protected ConversationMltQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis) {
        if (conversation.getMessages().isEmpty()) return null;

        // FIXME: compile mlt-request content
        final String content = conversation.getMessages().stream().sequential()
                .map(Message::getContent)
                .reduce(null, (s, e) -> {
                    if (s == null) return e;
                    return s + "\n\n" + e;
                });
        
        if(StringUtils.isBlank(content)){
            return null; //no content in the conversation to search for releated!
        }

        String displayTitle = StringUtils.defaultIfBlank(conf.getDisplayName(), conf.getName());
        if (StringUtils.isNotBlank(conversation.getContext().getDomain())) {
            displayTitle += " (" + conversation.getContext().getDomain() + ")";
        }
        return new ConversationMltQuery(getCreatorName(conf))
                .setInlineResultSupport(isResultSupported())
                .setDisplayTitle(displayTitle)
                .setConfidence(.55f)
                .setState(State.Suggested)
                .setContent(content);
    }

    @Override
    protected QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation, Analysis analysis, long offset, int pageSize, MultiValueMap<String, String> queryParams) {
        final ConversationMltQuery mltQuery = buildQuery(conf, intent, conversation, analysis);
        if (mltQuery == null) {
            return null;
        }

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("*").addField("score");
        solrQuery.addFilterQuery(String.format("%s:%s", FIELD_TYPE, TYPE_MESSAGE));
        solrQuery.addFilterQuery(String.format("%s:0",FIELD_MESSAGE_IDX));
        solrQuery.addSort("score", SolrQuery.ORDER.desc).addSort(FIELD_VOTE, SolrQuery.ORDER.desc);

        // #39 - paging
        solrQuery.setStart((int) offset);
        solrQuery.setRows(pageSize);

        //since #46 the client field is used to filter for the current user
        addClientFilter(solrQuery, conversation);

        addPropertyFilters(solrQuery, conversation, conf);

        return new ConversationMltRequest(solrQuery, mltQuery.getContent());

    }


}
