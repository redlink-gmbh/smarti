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
import io.redlink.smarti.model.*;
import io.redlink.smarti.services.InMemoryStoreService;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibEmbeddedAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("embedded")
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        ConversationSolrIT.EmbeddedSolrConfiguration.class, SolrLibEmbeddedAutoconfiguration.class,
        InMemoryStoreService.class, ConversationIndexer.class, ConversationSearchService.class})
public class ConversationSolrIT {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Autowired
    @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX)
    private SolrCoreDescriptor conversationCore;
    
    @Autowired
    private SolrCoreContainer solrServer;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ConversationSearchService searchService;

    @Autowired
    private ConversationIndexer conversationIndexer;
    
    @Before
    public void cleanSolr() throws Exception {
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
            assertThat(countDocs(), Matchers.equalTo(0L));
        }
    }

    @Test
    public void testEventPropagation() throws Exception {
        long docCount = countDocs();

        final Conversation conversation = buildConversation(new ObjectId(),"Servus Hasso, wie geht's denn so?");

        storeService.store(conversation);
        Thread.sleep(2 * conversationIndexer.getCommitWithin());

        assertThat(countDocs(), Matchers.equalTo(docCount));

        conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
        storeService.store(conversation);
        Thread.sleep(2 * conversationIndexer.getCommitWithin());
        assertThat(countDocs(), Matchers.greaterThan(docCount));
    }

    @Test
    public void testMlt() throws Exception {

        ObjectId owner = new ObjectId();

        final Conversation conversation1 = buildConversation(owner,"Das ist ein test");
        final Conversation conversation2 = buildConversation(owner,"Was anderes");

        conversation1.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation2.getMeta().setStatus(ConversationMeta.Status.Complete);

        final Conversation conversation3 = buildConversation(owner,"Das ist ein test");

        storeService.store(conversation1);
        storeService.store(conversation2);

        Thread.sleep(2 * conversationIndexer.getCommitWithin());

        assertThat(countDocs(), Matchers.equalTo(4L));

        ConversationMltQueryBuilder hassoMlt = new ConversationMltQueryBuilder(solrServer, conversationCore, null);

        // TODO does not make sense to build a query without a template...
        // hassoMlt.doBuildQuery(null, null, conversation3);

    }

    @Test
    public void testSearch() throws InterruptedException, IOException, SolrServerException {
        ObjectId owner = new ObjectId();
        ObjectId owner2 = new ObjectId();

        Client client = mock(Client.class);
        when(client.getId()).thenReturn(owner);

        final Conversation conversation1 = buildConversation(owner,"Das ist ein test", "Nochmal ein test", "Was anderes");
        final Conversation conversation2 = buildConversation(owner,"Was anderes");
        final Conversation conversation3 = buildConversation(owner,"test hallo");
        final Conversation conversation4 = buildConversation(owner2,"test hallo");

        conversation1.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation2.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation3.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation4.getMeta().setStatus(ConversationMeta.Status.Complete);

        final Conversation conversation5 = buildConversation(owner,"Das ist ein test");

        storeService.store(conversation1);
        storeService.store(conversation2);
        storeService.store(conversation3);
        storeService.store(conversation4);
        storeService.store(conversation5);

        solrServer.getSolrClient(conversationCore).commit();

        //create search
        MultiValueMap <String,String> query = new LinkedMultiValueMap<>();
        query.add("text", "test");
        query.add("group.limit", "3");
        query.add("hl", "true");
        query.add("hl.fl", "text");

        SearchResult<Conversation> result = searchService.search(client,query);

        assertEquals(2, result.getNumFound());
        assertEquals(2, result.getDocs().get(0).getMessages().size());
        assertEquals(1, result.getDocs().get(1).getMessages().size());

        MultiValueMap <String,String> query2 = new LinkedMultiValueMap<>();
        query2.add("text", "xyz");

        SearchResult<Conversation> result2 = searchService.search(client,query2);

        assertEquals(0, result2.getNumFound());

        solrServer.getSolrClient(conversationCore).deleteByQuery("*:*");
        solrServer.getSolrClient(conversationCore).commit();

        SearchResult<Conversation> result3 = searchService.search(client,query);

        assertEquals(0, result3.getNumFound());

    }

    private long countDocs() throws IOException, SolrServerException {
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final QueryResponse response = solrClient.query(new SolrQuery("*:*").setRows(0));
            return response.getResults().getNumFound();
        }
    }

    private Conversation buildConversation(ObjectId ownerId, String ... content) {
        final Conversation conversation = new Conversation();
        conversation.setOwner(ownerId);
        for(int i = 0; i < content.length; i++) {
            final Message m = new Message();
            m.setId(UUID.randomUUID().toString());
            m.setTime(Date.from(Instant.now().minusSeconds(60*(content.length-i))));
            m.setContent(content[i]);
            conversation.getMessages().add(m);
        }
        return conversation;
    }
    
    @Configuration
    @Import(ConversationIndexConfiguration.class) 
    static class EmbeddedSolrConfiguration {

        @Bean
        @Primary
        SolrLibProperties solrLibProperties() throws IOException {
            SolrLibProperties properties = new SolrLibProperties();

            properties.setHome(Files.createTempDirectory(temporaryFolder.getRoot().toPath(), "solr-home"));

            return properties;
        }

    }

}