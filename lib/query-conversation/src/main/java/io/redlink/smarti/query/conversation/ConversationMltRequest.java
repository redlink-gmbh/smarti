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

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 09.02.17.
 */
public class ConversationMltRequest extends QueryRequest {

    private String content;
    private Collection<ContentStream> _cs;

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
        if (_cs == null) {
            _cs = ClientUtils.toContentStreams(getContent(), ContentType.TEXT_PLAIN.toString());
        }
        return _cs;
    }

    @Override
    public RequestWriter.ContentWriter getContentWriter(String expectedType) {
        Collection<ContentStream> cs = getContentStreams();
        if (cs.isEmpty() || cs.size() > 1){
            return null;
        }
        ContentStream stream = cs.iterator().next();
        return new RequestWriter.ContentWriter() {
            @Override
            public void write(OutputStream os) throws IOException {
                IOUtils.copy(stream.getStream(), os);
            }

            @Override
            public String getContentType() {
                return stream.getContentType();
            }
        };
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}