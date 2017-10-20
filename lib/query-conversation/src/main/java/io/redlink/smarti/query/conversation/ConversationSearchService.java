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

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.SearchResult;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_OWNER;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_TYPE;

@Service
public class ConversationSearchService {

    private final SolrCoreContainer solrServer;
    private final SolrCoreDescriptor conversationCore;

    private final StoreService storeService;

    private final Function<SolrDocument,Conversation> solrDocTransformer;
    
    @Autowired
    public ConversationSearchService(SolrCoreContainer solrServer, @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore, StoreService storeService) {
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
        this.storeService = storeService;
        this.solrDocTransformer = (doc) -> storeService.get(new ObjectId(String.valueOf(doc.getFirstValue("id"))));
    }

    public SearchResult<Conversation> search(Client client, MultiValueMap<String, String> queryParams) throws IOException {
        return search(client, queryParams, Function.identity());
    }

    public <T> SearchResult<T> search(Client client, MultiValueMap<String, String> queryParams, Function<Conversation, T> convTransformer) throws IOException {

        final ModifiableSolrParams solrParams = new ModifiableSolrParams(toListOfStringArrays(queryParams, "text"));

        solrParams.add(CommonParams.FL, "id");
        if (client != null) {
            // FIXME: once authentication is ready, remove this IF
            solrParams.add(CommonParams.FQ, String.format("%s:\"%s\"", FIELD_OWNER,
                    ClientUtils.escapeQueryChars(client.getId().toHexString())));
        }
        solrParams.add(CommonParams.FQ, String.format("%s:\"%s\"", FIELD_TYPE, "conversation"));
        if (queryParams.containsKey("text")) {
            solrParams.set(CommonParams.Q, String.format("{!parent which=\"%s:%s\"}text:%s",
                    FIELD_TYPE, "conversation",
                    ClientUtils.escapeQueryChars(queryParams.getFirst("text"))));
        }

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {

            final QueryResponse queryResponse = solrClient.query(solrParams);

            return fromQueryResponse(queryResponse, solrDocTransformer.andThen(convTransformer));

        } catch (SolrServerException e) {
            throw new IllegalStateException("Cannot query non-initialized core", e);
        }
    }

    private static Map<String, String[]> toListOfStringArrays(Map<String, List<String>> in, String... excludes) {
        final Set<String> excludeKeys = new HashSet<>(Arrays.asList(excludes));
        final Map<String, String[]> map = new HashMap<>();
        in.forEach((k, v) -> {
            if (!excludeKeys.contains(k)) {
                map.put(k, v.toArray(new String[v.size()]));
            }
        });
        return map;
    }

    private static <T> SearchResult<T> fromQueryResponse(QueryResponse solrQueryResponse, Function<SolrDocument, T> resultMapper) {
        final SolrDocumentList results = solrQueryResponse.getResults();

        final SearchResult<T> searchResult = new SearchResult<>(results.getNumFound(), results.getStart(),
                results.stream().map(resultMapper).collect(Collectors.toList()));

        if (results.getMaxScore() != null) {
            searchResult.setMaxScore(results.getMaxScore());
        }

        return searchResult;
    }


}
