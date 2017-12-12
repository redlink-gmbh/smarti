package io.redlink.smarti.webservice;

import static java.nio.file.Files.createTempDirectory;

import java.io.IOException;
import java.util.Date;

import io.redlink.smarti.model.*;
import io.redlink.smarti.repositories.AuthTokenRepository;
import io.redlink.smarti.services.AuthTokenService;
import io.swagger.annotations.ApiParam;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.redlink.smarti.Application;
import io.redlink.smarti.model.ConversationMeta.Status;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.query.conversation.ConversationIndexConfiguration;
import io.redlink.smarti.repositories.ClientRepository;
import io.redlink.smarti.repositories.ConfigurationRepo;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes={Application.class,ConversationWebserviceIT.EmbeddedSolrConfiguration.class})
@ActiveProfiles("test")
//@WebAppConfiguration
//@EnableMongoRepositories(basePackageClasses={ConversationRepository.class, ClientRepository.class, ConfigurationRepo.class})
@EnableAutoConfiguration
public class ConversationWebserviceIT {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;
    
    @Autowired
    private ConfigurationRepo configurationRepo;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private ClientService clientService;

    @Autowired
    private AuthTokenService authTokenService;
    
    @Autowired
    private ConfigurationService configService;

    protected MockMvc mvc;
    
    private Client client;
    
    private Configuration clientConfig;
    private AuthToken authToken;

    @Before
    public void init(){
        mvc = MockMvcBuilders.webAppContextSetup(context)
//                .apply(springSecurity())
                .build();

        client = new Client();
        client.setName("test-client");
        client.setDescription("A Client created for testing");
        client.setDefaultClient(true);
        client = clientService.save(client);
        clientConfig = configService.createConfiguration(client);
        authToken = authTokenService.createAuthToken(client.getId(), "test");
    }
    
    @Test
    public void testSetup() throws Exception{
        Conversation conversation = new Conversation();
        conversation.setChannelId("test-channel-1");
        conversation.setOwner(client.getId());
        conversation.setMeta(new ConversationMeta());
        conversation.getMeta().setStatus(Status.New);
        conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, "test-channel-1");
        conversation.getMeta().setProperty(ConversationMeta.PROP_SUPPORT_AREA, "testing");
        conversation.getMeta().setProperty(ConversationMeta.PROP_TAGS, "test");
        conversation.setContext(new Context());
        conversation.getContext().setDomain("test-domain");
        conversation.getContext().setContextType("text-context");
        conversation.getContext().setEnvironment("environment-test", "true");
        conversation.setUser(new User("alois.tester"));
        conversation.getUser().setDisplayName("Alois Tester");
        conversation.getUser().setEmail("alois.tester@test.org");
        Message msg = new Message("test-channel-1-msg-1");
        msg.setContent("Wie kann ich das Smarti Conversation Service am besten Testen?");
        msg.setUser(conversation.getUser());
        msg.setOrigin(Origin.User);
        msg.setTime(new Date());
        conversation.getMessages().add(msg);
        conversation = conversationService.update(client, conversation);
        
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken.getToken())
//              .with(SecurityMockMvcRequestPostProcessors.anonymous())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200));
    }
    
    @After
    public void cleanRepos(){
        conversationRepository.deleteAll();
        clientRepository.deleteAll();
        configurationRepo.deleteAll();
        authTokenRepository.deleteAll();
    }
    
    @org.springframework.context.annotation.Configuration
    @Import(ConversationIndexConfiguration.class) 
    static class EmbeddedSolrConfiguration {

        @Bean
        @Primary
        SolrLibProperties solrLibProperties() throws IOException {
            SolrLibProperties properties = new SolrLibProperties();

            properties.setHome(createTempDirectory(temporaryFolder.getRoot().toPath(), "solr-home"));

            return properties;
        }

    }
}
