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

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.services.TemplateRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;

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
    protected ConversationResult toHassoResult(SolrDocument solrDocument, String type) {
        final ConversationResult hassoResult = new ConversationResult(getCreatorName());

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
    protected ConversationResult toHassoResult(SolrDocument question, SolrDocumentList answers, String type) {
        ConversationResult result = toHassoResult(question, type);
        for(SolrDocument answer : answers) {
            result.addAnswer(toHassoResult(answer,type));
        }
        return result;
    }

    @Override
    protected ConversationMltQuery buildQuery(ComponentConfiguration conf, Template intent, Conversation conversation) {
        if (conversation.getMessages().isEmpty()) return null;

        // FIXME: compile mlt-request content
        final String content = conversation.getMessages().stream().sequential()
                .map(Message::getContent)
                .reduce(null, (s, e) -> {
                    if (s == null) return e;
                    return s + "\n\n" + e;
                });

        String displayTitle = "Ähnliche Conversationen/Threads";
        if (StringUtils.isNotBlank(conversation.getContext().getDomain())) {
            displayTitle += " (" + conversation.getContext().getDomain() + ")";
        }
        return new ConversationMltQuery(getCreatorName())
                .setInlineResultSupport(isResultSupported())
                .setDisplayTitle(displayTitle)
                .setConfidence(.55f)
                .setState(State.Suggested)
                .setContent(content);
    }

    @Override
    protected QueryRequest buildSolrRequest(ComponentConfiguration conf, Template intent, Conversation conversation) {
        final ConversationMltQuery mltQuery = buildQuery(conf, intent, conversation);
        if (mltQuery == null) {
            return null;
        }

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("*").addField("score");
        solrQuery.addFilterQuery(String.format("%s:message",FIELD_TYPE));
        solrQuery.addFilterQuery(String.format("%s:0",FIELD_MESSAGE_IDX));
        solrQuery.addSort("score", SolrQuery.ORDER.desc).addSort(FIELD_VOTE, SolrQuery.ORDER.desc);

        final String domain = conversation.getContext().getDomain();
        if (StringUtils.isNotBlank(domain)) {
            solrQuery.addFilterQuery(String.format("%s:%s", FIELD_DOMAIN, ClientUtils.escapeQueryChars(domain)));
        } else {
             solrQuery.addFilterQuery(String.format("-%s:*", FIELD_DOMAIN));
        }

        return new ConversationMltRequest(solrQuery, mltQuery.getContent());

    }


}
