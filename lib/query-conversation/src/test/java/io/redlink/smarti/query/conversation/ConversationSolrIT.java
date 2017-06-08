package io.redlink.smarti.query.conversation;

import io.redlink.smarti.services.InMemoryStoreService;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibEmbeddedAutoconfiguration;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.query.conversation.ConversationMltQueryBuilder;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

import static org.junit.Assert.assertThat;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("embedded")
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        ConversationSolrIT.EmbeddedSolrConfiguration.class, SolrLibEmbeddedAutoconfiguration.class,
        InMemoryStoreService.class, ConversationIndexer.class})
@Ignore //FIXME this test does NOT work!
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

        final Conversation conversation = buildConversation("Servus Hasso, wie geht's denn so?");

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

        final Conversation conversation1 = buildConversation("Das ist ein test");
        final Conversation conversation2 = buildConversation("Was anderes");

        conversation1.getMeta().setStatus(ConversationMeta.Status.Complete);
        conversation2.getMeta().setStatus(ConversationMeta.Status.Complete);

        final Conversation conversation3 = buildConversation("Das ist ein test");

        storeService.store(conversation1);
        storeService.store(conversation2);

        Thread.sleep(2 * conversationIndexer.getCommitWithin());

        assertThat(countDocs(), Matchers.equalTo(4L));

        ConversationMltQueryBuilder hassoMlt = new ConversationMltQueryBuilder(solrServer, conversationCore, null);

        hassoMlt.doBuildQuery(null, conversation3);

    }

    private long countDocs() throws IOException, SolrServerException {
        try (SolrClient solrClient = solrServer.getSolrClient(conversationCore)) {
            final QueryResponse response = solrClient.query(new SolrQuery("*:*").setRows(0));
            return response.getResults().getNumFound();
        }
    }

    private Conversation buildConversation(String content) {
        final Conversation conversation = new Conversation();
        final Message m = new Message();
        m.setTime(new Date());
        m.setContent(content);
        conversation.getMessages().add(m);
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