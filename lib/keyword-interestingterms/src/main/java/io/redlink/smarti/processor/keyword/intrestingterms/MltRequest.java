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