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

package io.redlink.smarti.lib.solr.iterms;

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.RequestWriter.ContentWriter;
import org.apache.solr.common.params.SolrParams;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @author Rupert Westenthaler
 * @since 09.02.2017
 */
public class MltRequest extends QueryRequest {

    private static final long serialVersionUID = -7427766356768368678L;

    private String content;
    private final String path;

    public MltRequest(SolrParams q, String content) {
        this(q, null, content);
    }
    public MltRequest(SolrParams q, String path, String content) {
        super(q, METHOD.POST);
        this.setContent(content);
        this.path = path == null ? "/mlt" : path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ContentWriter getContentWriter(String expectedType) {
        return new RequestWriter.StringPayloadContentWriter(content, expectedType);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}