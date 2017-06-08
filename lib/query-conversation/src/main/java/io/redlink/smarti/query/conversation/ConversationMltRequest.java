package io.redlink.smarti.query.conversation;

import org.apache.http.entity.ContentType;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

import java.util.Collection;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 09.02.17.
 */
public class ConversationMltRequest extends QueryRequest {

    private String content;

    public ConversationMltRequest(SolrParams q, String content) {
        super(q, METHOD.POST);
        this.setContent(content);
    }

    @Override
    public String getPath() {
        return "/mlt";
    }

    @Override
    public Collection<ContentStream> getContentStreams() {
        return ClientUtils.toContentStreams(getContent(), ContentType.TEXT_PLAIN.toString());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}