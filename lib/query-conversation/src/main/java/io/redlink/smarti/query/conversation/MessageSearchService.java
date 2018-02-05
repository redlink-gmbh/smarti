package io.redlink.smarti.query.conversation;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

@Service
public class MessageSearchService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SolrCoreContainer solrServer;
    private final SolrCoreDescriptor conversationCore;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    public MessageSearchService(SolrCoreContainer solrServer, @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore) {
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
    }

    public Object search(Set<ObjectId> clientIds, MultiValueMap<String, String> requestParameterMap) throws IOException {
        final ModifiableSolrParams solrParams = new ModifiableSolrParams();
        requestParameterMap.entrySet().forEach(e -> solrParams.set(e.getKey(), e.getValue().toArray(new String[0])));

        clientIds.stream().map(ObjectId::toHexString).reduce((a,b) -> a + " OR " + b).ifPresent(clientFilter -> {
            solrParams.add(CommonParams.FQ, String.format("%s:(%s)", FIELD_OWNER, clientFilter));
        });
        solrParams.add(CommonParams.FQ, String.format("%s:\"%s\"", FIELD_TYPE, TYPE_MESSAGE));

        log.trace("SolrParams: {}", solrParams);

        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {

            QueryResponse response = solrClient.query(solrParams);

            Map map = response.getResponse().asMap(Integer.MAX_VALUE);

            map.put("meta", ImmutableMap.of("numFound", response.getResults().getNumFound(), "start", response.getResults().getStart()));

            return map;

        } catch (SolrServerException e) {
            throw new IllegalStateException("Cannot query non-initialized core", e);
        }
    }

}
