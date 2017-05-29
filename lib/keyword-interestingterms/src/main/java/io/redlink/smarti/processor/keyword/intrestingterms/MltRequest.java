package io.redlink.smarti.processor.keyword.intrestingterms;

import org.apache.http.entity.ContentType;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 */
class MltRequest extends QueryRequest {

    private List<String> contents = new LinkedList<>();

    public MltRequest(SolrParams q) {
        this(q, null);
    }
    
    public MltRequest(SolrParams q, String content) {
        super(q, METHOD.POST);
        if(content != null){
            this.contents.add(content);
        }
    }

    public List<String> getContents() {
        return contents;
    }
    
    public void setContents(List<String> contents) {
        this.contents = contents == null ? new LinkedList<>() : contents;
    }
    
    public void addContent(String content){
        this.contents.add(content);
    }
    
    @Override
    public String getPath() {
        return "/mlt";
    }

    @Override
    public Collection<ContentStream> getContentStreams() {
        return ClientUtils.toContentStreams(contents.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n")), ContentType.TEXT_PLAIN.toString());
    }

}