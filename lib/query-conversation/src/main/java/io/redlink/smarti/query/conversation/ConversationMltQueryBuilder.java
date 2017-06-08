package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.*;
import io.redlink.smarti.services.IntendRegistry;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 09.02.17.
 */
@Component
public class ConversationMltQueryBuilder extends ConversationQueryBuilder {

    public static final String CREATOR_NAME = "Hasso-MLT";

    @Autowired
    public ConversationMltQueryBuilder(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore, 
            IntendRegistry registry) {
        super(CREATOR_NAME, solrServer, conversationCore, registry);
    }

    @Override
    protected ConversationResult toHassoResult(SolrDocument solrDocument, MessageTopic type) {
        final ConversationResult hassoResult = new ConversationResult(getCreatorName(), type);
        hassoResult.setScore(Double.parseDouble(String.valueOf(solrDocument.getFieldValue("score"))));
        hassoResult.setContent(String.valueOf(solrDocument.getFirstValue("message")));
        hassoResult.setReplySuggestion(hassoResult.getContent());
        hassoResult.setConversationId(String.valueOf(solrDocument.getFieldValue("conversation_id")));
        hassoResult.setMessageIdx(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("message_idx"))));
        hassoResult.setVotes(Integer.parseInt(String.valueOf(solrDocument.getFieldValue("vote"))));
        return hassoResult;
    }

    @Override
    protected ConversationMltQuery buildQuery(Intend intend, Conversation conversation) {
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
    protected QueryRequest buildSolrRequest(Intend intend, Conversation conversation) {
        final ConversationMltQuery mltQuery = buildQuery(intend, conversation);
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
