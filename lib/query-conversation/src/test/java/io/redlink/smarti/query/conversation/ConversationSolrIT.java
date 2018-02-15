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

import io.redlink.smarti.model.*;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.services.ConversationService;
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
import org.junit.After;
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
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
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
        ConversationService.class, ConversationIndexer.class, ConversationSearchService.class})
@EnableMongoRepositories(basePackageClasses={ConversationRepository.class})
public class ConversationSolrIT {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Autowired
    @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX)
    private SolrCoreDescriptor conversationCore;
    
    @Autowired
    private SolrCoreContainer solrServer;

    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationSearchService searchService;

    @Autowired
    private ConversationIndexer conversationIndexer;
    
    private Client client;
    
    @Before
    public void cleanSolr() throws Exception {
        client = new Client();
        client.setId(new ObjectId());
        client.setName("Test Client");
        client.setDescription("A Client created for testing");
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
            assertThat(countDocs(), Matchers.equalTo(0L));
        }
    }

    @After
    public void cleanConversationRepo() {
        conversationRepository.deleteAll();
    }
    
    @Test
    public void testEventPropagation() throws Exception {
        long docCount = countDocs();

        final Conversation conversation = buildConversation(client,"Servus Hasso, wie geht's denn so?");

        conversationService.update(client, conversation);
        Thread.sleep(2 * conversationIndexer.getCommitWithin());

        assertThat(countDocs(), Matchers.equalTo(docCount));

        conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversationService.update(client, conversation);
        Thread.sleep(2 * conversationIndexer.getCommitWithin());
        assertThat(countDocs(), Matchers.greaterThan(docCount));
    }

    @Test
    public void testMlt() throws Exception {

        ObjectId owner = new ObjectId();

        final Conversation conversation1 = buildConversation(client,"Das ist ein test");
        final Conversation conversation2 = buildConversation(client,"Was anderes");

        conversation1.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation2.getMeta().setStatus(ConversationMeta.Status.Complete);

        final Conversation conversation3 = buildConversation(client,"Das ist ein test");

        conversationService.update(client, conversation1);
        conversationService.update(client, conversation2);

        Thread.sleep(2 * conversationIndexer.getCommitWithin());

        assertThat(countDocs(), Matchers.equalTo(4L));

        ConversationMltQueryBuilder hassoMlt = new ConversationMltQueryBuilder(solrServer, conversationCore, null);

        // TODO does not make sense to build a query without a template...
        // hassoMlt.doBuildQuery(null, null, conversation3);

    }

    @Test
    public void testSearch() throws InterruptedException, IOException, SolrServerException {

        Client client2 = new Client();
        client2.setId(new ObjectId());
        client2.setName("Test Client 2");

        final Conversation conversation1 = buildConversation(client,"Das ist ein test", "Nochmal ein test", "Was anderes");
        final Conversation conversation2 = buildConversation(client,"Was anderes");
        final Conversation conversation3 = buildConversation(client,"test hallo");
        final Conversation conversation4 = buildConversation(client2,"test hallo");

        conversation1.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation2.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation3.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation4.getMeta().setStatus(ConversationMeta.Status.Complete);

        final Conversation conversation5 = buildConversation(client,"Das ist ein test");

        conversationService.update(client, conversation1);
        conversationService.update(client, conversation2);
        conversationService.update(client, conversation3);
        conversationService.update(client2, conversation4);
        conversationService.update(client, conversation5);

        solrServer.getSolrClient(conversationCore).commit();

        //create search
        MultiValueMap <String,String> query = new LinkedMultiValueMap<>();
        query.add("text", "test");
        query.add("group.limit", "3");
        query.add("hl", "true");
        query.add("hl.fl", "text");

        SearchResult<ConversationSearchService.ConversationResult> result = searchService.search(client,query);

        assertEquals(2, result.getNumFound());
        assertEquals(1, result.getDocs().get(0).getResults().size()); //one result
        assertEquals(2, result.getDocs().get(0).getResults().get(0).getMessages().size()); //but with matches in 2 messages
        assertEquals(1, result.getDocs().get(1).getResults().size());

        MultiValueMap <String,String> query2 = new LinkedMultiValueMap<>();
        query2.add("text", "xyz");

        SearchResult<ConversationSearchService.ConversationResult> result2 = searchService.search(client,query2);

        assertEquals(0, result2.getNumFound());

        solrServer.getSolrClient(conversationCore).deleteByQuery("*:*");
        solrServer.getSolrClient(conversationCore).commit();

        SearchResult<ConversationSearchService.ConversationResult> result3 = searchService.search(client,query);

        assertEquals(0, result3.getNumFound());

    }

    private long countDocs() throws IOException, SolrServerException {
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final QueryResponse response = solrClient.query(new SolrQuery("*:*").setRows(0));
            return response.getResults().getNumFound();
        }
    }

    private Conversation buildConversation(Client client, String ... content) {
        final Conversation conversation = new Conversation();
        conversation.setOwner(client.getId());
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