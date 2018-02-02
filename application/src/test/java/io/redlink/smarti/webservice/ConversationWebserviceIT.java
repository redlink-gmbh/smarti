package io.redlink.smarti.webservice;

import static java.nio.file.Files.createTempDirectory;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import io.redlink.smarti.model.*;
import io.redlink.smarti.repositories.AuthTokenRepository;
import io.redlink.smarti.services.AuthTokenService;
import io.redlink.smarti.services.AuthenticationService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bson.types.ObjectId;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.redlink.smarti.Application;
import io.redlink.smarti.health.MongoHealthCheck;
import io.redlink.smarti.health.MongoHealthCheck.DbVersion;
import io.redlink.smarti.model.ConversationMeta.Status;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.query.conversation.ConversationIndexConfiguration;
import io.redlink.smarti.query.conversation.ConversationMltQueryBuilder;

import static io.redlink.smarti.health.MongoHealthCheck.COLLECTION_NAME;
import static io.redlink.smarti.health.MongoHealthCheck.EXPECTED_DB_VERSION;
import static io.redlink.smarti.health.MongoHealthCheck.SMARTI_DB_VERSION_ID;
import static io.redlink.smarti.query.conversation.RelatedConversationTemplateDefinition.RELATED_CONVERSATION_TYPE;
import io.redlink.smarti.repositories.ClientRepository;
import io.redlink.smarti.repositories.ConfigurationRepo;
import io.redlink.smarti.repositories.ConversationRepository;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.webservice.pojo.CallbackPayload;
import io.redlink.smarti.webservice.pojo.ConversationData;
import io.redlink.solrlib.spring.boot.autoconfigure.SolrLibProperties;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes={Application.class,ConversationWebserviceIT.EmbeddedSolrConfiguration.class, ConversationWebserviceIT.SmartiDbVersionInitializer.class})
@ActiveProfiles("test")
//@WebAppConfiguration
//@EnableMongoRepositories(basePackageClasses={ConversationRepository.class, ClientRepository.class, ConfigurationRepo.class})
@EnableAutoConfiguration
public class ConversationWebserviceIT {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Pattern LINK_HEADER_PATTERN = Pattern.compile("<(.*)>; rel=\"(.*)\"");
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
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

    @MockBean
    private CallbackService callbackService;
    
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
 
        Configuration config = configService.getDefaultConfiguration();
        config.getConfig().values().stream().flatMap(cl -> cl.stream()).forEach(cc -> cc.setEnabled(true));
        clientConfig = configService.storeConfiguration(client, config.getConfig());
        authToken = authTokenService.createAuthToken(client.getId(), "test");
    }
    
    @Test
    public void testGetConversation() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200));
        
        //Now create a 2nd client
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        configService.storeConfiguration(client2, configService.getDefaultConfiguration().getConfig());
        AuthToken authToken2 = authTokenService.createAuthToken(client2.getId(), "test");
        
        //and test that it can NOT access the conversation of the other one
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken2.getToken()) //Not the owner
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is4xxClientError()); //404, 403
        
    }
    
    @Test
    public void testDeleteConversation() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
        
        //Now create a 2nd client
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        configService.storeConfiguration(client2,configService.getDefaultConfiguration().getConfig());
        AuthToken authToken2 = authTokenService.createAuthToken(client2.getId(), "test");
        
        //and test that it can NOT delete the conversation of the other one
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken2.getToken()) //Not the owner
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is4xxClientError()); //404, 403

        //now delete the conversation
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        
        //Assert that the conversation was deleted
        Assert.assertNull(conversationService.getConversation(conversation.getId()));
        
    }
    
    @Test
    public void testModifyConversationField() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
       
        //update a meta field
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.myTest")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("\"initial value\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.myTest").value("initial value"));
        //reset the value and also test without the optional meta prefix
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/myTest")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("\"changed value\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.myTest").value("changed value"));
        //test if the value is persited by retrieval of the conversation
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.myTest").value("changed value"));

        //test update fails with wrong user
        //Now create a 2nd client
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        configService.storeConfiguration(client2,configService.getDefaultConfiguration().getConfig());
        AuthToken authToken2 = authTokenService.createAuthToken(client2.getId(), "test");
        
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.myTest")
                .header("X-Auth-Token", authToken2.getToken())
                .content("\"unexpected value\"")
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        //assert that the value was not set and the value is still "changed value"
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + conversation.getId())
                .header("X-Auth-Token", authToken.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andExpect(jsonPath("meta.myTest").value("changed value"));
        //set multiple values to the field
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/myTest")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("[\"first value\",\"second value\"]")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.myTest",Matchers.contains("first value","second value")));

        //Delete a field
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + conversation.getId()+"/myTest")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.myTest").doesNotExist());

        //special case status
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.status")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("\""+Status.Ongoing+"\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.status").value(Status.Ongoing.name()));
        //set status to an invalid value
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.status")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("\"Unknown\"") //unknown is invalid
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        //try multiple values
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.status")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("[\"" + Status.New + "\",\"" + Status.Complete + "\"]") //unknown is invalid
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        //complete the conversation
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/status")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .content("\""+Status.Complete+"\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("meta.status").value(Status.Complete.name()));
        //try to delete the status (Not allowed)
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + conversation.getId()+"/status")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + conversation.getId()+"/meta.status")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        
        //Test callbacks and analysis setting

        String callbackURI = "http://www.example.org/smarti/callback/test";

        
        ArgumentCaptor<CallbackPayload> callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        ArgumentCaptor<URI> callbackUriCapture = ArgumentCaptor.forClass(URI.class);

        //per default analysis is false ... so no callback will happen
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.callbackTest")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .param("analysis", "true") //include analysis (will wait until processing is done)
                .content("\"some value\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(jsonPath("analysis").isMap())
                .andExpect(jsonPath("analysis.tokens").isArray())
                .andExpect(jsonPath("analysis.templates").isArray());
        
        //now parse analysis=true to re-analyse the conversation after the field update
         Conversation c = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + conversation.getId()+"/meta.callbackTest")
                .header("X-Auth-Token", authToken.getToken())
                .header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                //.param("analysis", "false") //assert this is the default
                .param("callback", callbackURI)
                .content("\"some value\"")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(jsonPath("analysis").doesNotExist()) //no analysis included!
                .andReturn().getResponse().getContentAsString(),Conversation.class);
        
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(1)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        CallbackPayload<?> payload = callbackPayloadCapture.getValue();
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        Analysis analysis = (Analysis) payload.getData();
        Assert.assertEquals(c.getId(), analysis.getConversation());
        Assert.assertEquals(c.getLastModified(), analysis.getDate());
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertNotNull(analysis.getTemplates());

    }
    
    @Test
    public void testListConversations() throws Exception{
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        configService.storeConfiguration(client2,configService.getDefaultConfiguration().getConfig());
        AuthToken authToken2 = authTokenService.createAuthToken(client2.getId(), "test");

        Map<String,Conversation> client1Conversations = new HashMap<>();
        Map<String,Conversation> client2Conversations = new HashMap<>();
        
        for(int i = 0 ; i < 20; i++){
            Conversation conversation = new Conversation();
            conversation.setOwner(i%2 == 0 ? client.getId() : client2.getId());
            conversation.setMeta(new ConversationMeta());
            conversation.getMeta().setStatus(Status.New);
            conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, "test-channel-" + i%2);
            conversation.getMeta().setProperty(ConversationMeta.PROP_SUPPORT_AREA, "testing");
            conversation.getMeta().setProperty(ConversationMeta.PROP_TAGS, "test");
            conversation.setContext(new Context());
            conversation.getContext().setDomain("test-domain-client-" + i%2);
            conversation.getContext().setContextType("text-context-client-" + i%2);
            conversation.getContext().setEnvironment("environment-test", "true");
            conversation.setUser(new User((i%2 == 0 ? "alois" : "zodar") + ".tester"));
            conversation.getUser().setDisplayName((i%2 == 0 ? "Alois" : "Zodar") + " Tester");
            conversation.getUser().setEmail((i%2 == 0 ? "alois" : "zodar") + ".tester@test.org");
            Message msg = new Message("test-channel-" + i%2 + "-msg-1");
            msg.setContent("Wie kann " + (i%2 == 0 ? "Alois" : "Zodar") + " das Smarti Conversation Service am besten Testen?");
            msg.setUser(conversation.getUser());
            msg.setOrigin(Origin.User);
            msg.setTime(new Date());
            conversation.getMessages().add(msg);
            conversation = conversationService.update(i%2 == 0 ? client : client2, conversation);
            (i%2 == 0 ? client1Conversations : client2Conversations).put(conversation.getId().toHexString(), conversation);
        }
        //listing conversations without any restriction should return all conversations of the client
        //identified by the auth-token
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation")
                .header("X-Auth-Token", authToken.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andExpect(jsonPath("content[*].id", Matchers.containsInAnyOrder(client1Conversations.keySet().toArray(new String[]{}))))
              .andExpect(jsonPath("totalElements").value(10)) //test the paging metadata
              .andExpect(jsonPath("last").value(true))
              .andExpect(jsonPath("size").value(10))
              .andExpect(jsonPath("number").value(0))
              .andExpect(jsonPath("first").value(true))
              .andExpect(jsonPath("numberOfElements").value(10));
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation")
                .header("X-Auth-Token", authToken2.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andExpect(jsonPath("content[*].id", Matchers.containsInAnyOrder(client2Conversations.keySet().toArray(new String[]{}))));

        //test more pageable 
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("page", "2") //3rd page
                .param("size", "3")
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andExpect(jsonPath("totalElements").value(10)) //test the paging metadata
              .andExpect(jsonPath("last").value(false))
              .andExpect(jsonPath("size").value(3))
              .andExpect(jsonPath("number").value(2))
              .andExpect(jsonPath("first").value(false))
              .andExpect(jsonPath("numberOfElements").value(3));
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("page", "3") //4th page
                .param("size", "3")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("totalElements").value(10)) //test the paging metadata
                .andExpect(jsonPath("last").value(true))
                .andExpect(jsonPath("size").value(3))
                .andExpect(jsonPath("number").value(3))
                .andExpect(jsonPath("first").value(false))
                .andExpect(jsonPath("numberOfElements").value(1));
        
        //test an invalid call (auth-token of client1 and clientId of client2)
        //expected ??
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("client", client2.getId().toHexString())
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("totalElements").value(0)) //test the paging metadata
                .andExpect(jsonPath("last").value(true))
                .andExpect(jsonPath("size").value(10))
                .andExpect(jsonPath("number").value(0))
                .andExpect(jsonPath("first").value(true))
                .andExpect(jsonPath("numberOfElements").value(0));

    }
    
    @Test
    public void testCreateConversation() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId();
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse();
        
        Map<String,String> links = parseLinks(response.getHeaders("link"));
        
        Conversation created = objectMapper.readValue(response.getContentAsString(),Conversation.class);
                
        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());

        //Assert that the link header contain the expected path
        links.values().forEach(l -> Assert.assertTrue(l.contains("/conversation/" + created.getId())));
        
        //assert that we can retrieve the created conversation
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + created.getId())
                .header("X-Auth-Token", authToken.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200));

        
    }
    
    @Test
    public void testCreateConversationAsyncAnalysisCallback() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        String callbackURI = "http://www.example.org/smarti/callback/test";
        
        Conversation created = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse().getContentAsString(),Conversation.class);
       
        //now wait for the callback!
        ArgumentCaptor<CallbackPayload> callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        ArgumentCaptor<URI> callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(1)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        CallbackPayload<?> payload = callbackPayloadCapture.getValue();
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        Analysis analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertEquals(created.getLastModified(), analysis.getDate());
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertNotNull(analysis.getTemplates());
        //NOTE the following two assertions are only possible because analysis is captured from the mock 
        //     and never JSON serialized. Both the analysis ID and the Client are @JsonIgnored!
        Assert.assertNotNull(analysis.getId());
        Assert.assertEquals(conversation.getOwner(), analysis.getClient()); //NOTE assert to conversaion.getOwner() and NOT created.getOwner() as the owner is @JsonIgnored!
    }
    
    @Test
    public void testCreateConversationWithInclAnalysis() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
        msg.setContent("Ich fahre mit Peter Tester von München nach Berlin");
        msg.setUser(conversation.getUser());
        msg.setOrigin(Origin.User);
        msg.setTime(new Date());
        conversation.getMessages().add(msg);
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);

        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .param("analysis", "true")
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse();
        
        Map<String,String> links = parseLinks(response.getHeaders("link"));
        //NOTE we need to use ConversationData as Conversation does not support Analysis!
        ConversationData created = objectMapper.readValue(response.getContentAsString(),ConversationData.class);
        Assert.assertNotNull(created.getAnalysis());
        Assert.assertNotNull(created.getAnalysis().getTokens());
        Assert.assertNotNull(created.getAnalysis().getTemplates());
    }
    
    @Test
    public void testCreateConversationAnalysisRequest() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
        msg.setContent("Ich fahre mit Peter Tester von München nach Berlin");
        msg.setUser(conversation.getUser());
        msg.setOrigin(Origin.User);
        msg.setTime(new Date());
        conversation.getMessages().add(msg);
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);

        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse();
        
        Map<String,String> links = parseLinks(response.getHeaders("link"));
        
        Conversation created = objectMapper.readValue(response.getContentAsString(),Conversation.class);
        
        String analyseLink = links.get("analyse");
        Assert.assertNotNull(analyseLink);
        //now request the analysis
        Analysis analysis = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.get(analyseLink)
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"up\""))))
                .andExpect(jsonPath("id").doesNotExist()) //@JsonIgnore
                .andExpect(jsonPath("client").doesNotExist()) //@JsonIgnore
                .andExpect(jsonPath("conversation").value(created.getId().toHexString()))
                .andExpect(jsonPath("date").value(created.getLastModified()))
                .andExpect(jsonPath("tokens").isArray())
                .andExpect(jsonPath("templates").isArray())
                .andReturn().getResponse().getContentAsString(), Analysis.class);
        //this just asserts that Analysis supports both parse and read
        Assert.assertNotNull(analysis);
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertEquals(created.getLastModified(), analysis.getDate());
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertNotNull(analysis.getTemplates());
        //TODO: test token and Template requests
        
        this.mvc.perform(MockMvcRequestBuilders.get(analyseLink + "/token")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"analyse\""),Matchers.containsString("rel=\"up\""))))
                .andExpect(jsonPath("[0].value").value("Peter Tester"))
                .andExpect(jsonPath("[0].messageIdx").value(0))
                .andExpect(jsonPath("[0].start").value(14))
                .andExpect(jsonPath("[0].end").value(26))
                .andExpect(jsonPath("[0].origin").value("System"))
                .andExpect(jsonPath("[0].state").value("Suggested"))
                .andExpect(jsonPath("[0].type").value("Person"))
                .andExpect(jsonPath("[1].value").value("München"))
                .andExpect(jsonPath("[1].messageIdx").value(0))
                .andExpect(jsonPath("[1].start").value(31))
                .andExpect(jsonPath("[1].end").value(38))
                .andExpect(jsonPath("[1].origin").value("System"))
                .andExpect(jsonPath("[1].state").value("Suggested"))
                .andExpect(jsonPath("[1].type").value("Place"))
                .andExpect(jsonPath("[2].value").value("Berlin"))
                .andExpect(jsonPath("[2].messageIdx").value(0))
                .andExpect(jsonPath("[2].start").value(44))
                .andExpect(jsonPath("[2].end").value(50))
                .andExpect(jsonPath("[2].origin").value("System"))
                .andExpect(jsonPath("[2].state").value("Suggested"))
                .andExpect(jsonPath("[2].type").value("Place"));
    }
    
    @Test
    public void testCreateConversationAndAddMessage() throws Exception{
        Conversation conversation = new Conversation();
        //conversation.setChannelId("test-channel-1");
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        String callbackURI = "http://www.example.org/smarti/callback/test";
        //create the conversation with the initial message
        Conversation created = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse().getContentAsString(),Conversation.class);
       
        Message response = new Message();
        response.setContent("Am besten mittels einem Integrationstest.");
        response.setUser(new User("cora.tester"));
        response.getUser().setDisplayName("Cora Tester");
        response.getUser().setEmail("cora.tester@test.org");
        response.setOrigin(Origin.User);
        response.setTime(new Date());
        //update the conversation by adding the response
        Message createdMessage = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.post("/conversation/" + created.getId() + "/message")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(response)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""), Matchers.containsString("rel=\"up\""), Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse().getContentAsString(),Message.class);
        
        Assert.assertNotNull(createdMessage.getId());
        Assert.assertEquals(response.getContent(), createdMessage.getContent());
        Assert.assertNotNull(createdMessage.getUser());
        Assert.assertEquals(response.getUser().getId(), createdMessage.getUser().getId());
        
        Conversation updated = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + created.getId())
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andReturn().getResponse().getContentAsString(),Conversation.class);
        
        Assert.assertEquals(created.getId(), updated.getId());
        Assert.assertTrue(created.getLastModified().before(updated.getLastModified()));
        Assert.assertEquals(2,updated.getMessages().size());
        //check the messages and IDs
        Assert.assertEquals(msg.getId(), updated.getMessages().get(0).getId());
        Assert.assertEquals(createdMessage.getId(), updated.getMessages().get(1).getId());
        
        //now wait for the 2 callbacks (first for the creation; second for the appended message)!
        ArgumentCaptor<CallbackPayload> callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        ArgumentCaptor<URI> callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(2)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        //Assert the analysis of the original request
        callbackUriCapture.getAllValues().forEach(cb -> Assert.assertEquals(callbackURI, cb.toString()));
        Set<ObjectId> analysisIds = new HashSet<>();
        callbackPayloadCapture.getAllValues().forEach(payload -> {
            Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
            Assert.assertTrue(payload.getData() instanceof Analysis);
            Analysis analysis = (Analysis) payload.getData();
            Assert.assertEquals(created.getId(), analysis.getConversation());
            Assert.assertNotNull(analysis.getDate());
            //check that the dates correspond with the dates of the creates/updated conversation
            Assert.assertTrue(String.format("got analysis with unexpected date %s (created: %s, updated: %s",
                        analysis.getDate().toInstant(), created.getLastModified().toInstant(), updated.getLastModified().toInstant()),
                    Arrays.asList(created.getLastModified(), updated.getLastModified()).contains(analysis.getDate()));
            Assert.assertNotNull(analysis.getTokens());
            Assert.assertNotNull(analysis.getTemplates());
            //NOTE the following two assertions are only possible because analysis is captured from the mock 
            //     and never JSON serialized. Both the analysis ID and the Client are @JsonIgnored!
            Assert.assertNotNull(analysis.getId());
            analysisIds.add(analysis.getId());
            Assert.assertEquals(conversation.getOwner(), analysis.getClient()); //NOTE assert to conversaion.getOwner() and NOT created.getOwner() as the owner is @JsonIgnored!
        });
        Assert.assertEquals(1, analysisIds.size()); //both analysis MUST HAVE the same ID
    }
    
    @Test
    public void testMessageCrudOperations() throws Exception {
        String callbackURI = "http://www.example.org/smarti/callback/test";

        
        //create a new conversation by calling POST without payload
        Conversation created = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE))
                //.content(/* no payload!! */))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse().getContentAsString(),Conversation.class);
        
        Assert.assertNotNull(created.getId()); //Assert that we have an ID for the created conversation
        
        //We also test callbacks herem, as we need to assert that this works for an empty conversation
        ArgumentCaptor<CallbackPayload> callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        ArgumentCaptor<URI> callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(1)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        CallbackPayload<?> payload = callbackPayloadCapture.getValue();
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        Analysis analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertEquals(created.getLastModified(), analysis.getDate());
        //Assert that tokens are NOT NULL but empty
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertTrue(analysis.getTokens().isEmpty());
        //Templates need also te be NOT NULL. However even an empty conversation might have some templates
        Assert.assertNotNull(analysis.getTemplates());
        
        //NOW lets add an message
        Message msg = new Message(UUID.randomUUID().toString());
        msg.setContent("Ich besuche moregen Andreas Müller in Berlin."); //make sure wie have some Tokens extracted
        
        User aloisTester = new User("alois.tester");
        aloisTester.setDisplayName("Alois Tester");
        aloisTester.setEmail("alois.tester@test.org");

        msg.setUser(aloisTester);
        msg.setOrigin(Origin.User);
        msg.setTime(new Date());
        
        this.mvc.perform(MockMvcRequestBuilders.post("/conversation/" + created.getId().toHexString() + "/message")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(msg)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(jsonPath("id").value(msg.getId()))
                .andExpect(jsonPath("content").value(msg.getContent()))
                .andExpect(jsonPath("time").value(msg.getTime()))
                .andExpect(jsonPath("user.id").value(msg.getUser().getId()))
                .andExpect(jsonPath("user.displayName").value(msg.getUser().getDisplayName()))
                .andExpect(jsonPath("user.email").value(msg.getUser().getEmail()))
                .andExpect(jsonPath("votes").value(0))
                .andExpect(jsonPath("metadata").isMap())
                .andExpect(jsonPath("private").value(false))
                
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""),Matchers.containsString("rel=\"up\""))));
        
        //now consume the analysis results
        callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(2)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        payload = callbackPayloadCapture.getValue();
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertTrue(analysis.getDate().after(created.getLastModified())); //the analysis of a newer version
        //Assert that tokens are NOT NULL but empty
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertTrue(analysis.getTokens().size() > 0);
        //Assert that Berlin is extracted as Token
        Assert.assertTrue(analysis.getTokens().stream().anyMatch(t -> Objects.equals("Berlin", t.getValue())));
        //Templates need also t0 be NOT NULL and NOT empty!
        Assert.assertNotNull(analysis.getTemplates());
        Assert.assertTrue(analysis.getTemplates().size() > 0);

        //now update the content of message
        msg.setContent("Ich besuche moregen Andreas Müller in Bern.");
        msg.getMetadata().put("lastModified", new Date()); //not the modified date as a metadata item
        
        this.mvc.perform(MockMvcRequestBuilders.put("/conversation/" + created.getId().toHexString() + "/message/" + msg.getId())
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(msg)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("id").value(msg.getId()))
                .andExpect(jsonPath("content").value(msg.getContent())) //updated content
                .andExpect(jsonPath("time").value(msg.getTime())) //updated time
                .andExpect(jsonPath("user.id").value(msg.getUser().getId()))
                .andExpect(jsonPath("user.displayName").value(msg.getUser().getDisplayName()))
                .andExpect(jsonPath("user.email").value(msg.getUser().getEmail()))
                .andExpect(jsonPath("votes").value(0))
                .andExpect(jsonPath("metadata").isMap())
                .andExpect(jsonPath("metadata.lastModified").value(msg.getMetadata().get("lastModified"))) //assert the last modified date
                .andExpect(jsonPath("private").value(false))
                
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""),Matchers.containsString("rel=\"up\""))));
        
        //now consume the analysis results and make sure Berlin is no longer a Token AND Bern is found!
        callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(3)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        payload = callbackPayloadCapture.getValue(); //gets the result of the last call
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertTrue(analysis.getDate().after(created.getLastModified())); //the analysis of a newer version
        //Assert that tokens are NOT NULL but empty
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertTrue(analysis.getTokens().size() > 0);
        //Assert that Berlin is no longer an extracted as Token
        Assert.assertFalse(analysis.getTokens().stream().anyMatch(t -> Objects.equals("Berlin", t.getValue())));
        //Assert that Bern is no extracted as Token
        Assert.assertTrue(analysis.getTokens().stream().anyMatch(t -> Objects.equals("Bern", t.getValue())));
        //Templates need also t0 be NOT NULL and NOT empty!
        Assert.assertNotNull(analysis.getTemplates());
        Assert.assertTrue(analysis.getTemplates().size() > 0);
        
        
        //add a 2nd message form an other user
        Message msg2 = new Message(UUID.randomUUID().toString());
        msg2.setContent("Ich bin morgen auch in Bern, dann können wir uns eventuell treffen.");
        
        User peterTester = new User("peter.tester");
        peterTester.setDisplayName("Peter Tester");
        peterTester.setEmail("peter.tester@test.org");

        msg2.setUser(peterTester);
        msg2.setOrigin(Origin.User);
        msg2.setTime(new Date());
        
        this.mvc.perform(MockMvcRequestBuilders.post("/conversation/" + created.getId().toHexString() + "/message")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(msg2)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andExpect(jsonPath("id").value(msg2.getId()))
                .andExpect(jsonPath("content").value(msg2.getContent()))
                .andExpect(jsonPath("time").value(msg2.getTime()))
                .andExpect(jsonPath("user.id").value(msg2.getUser().getId()))
                .andExpect(jsonPath("user.displayName").value(msg2.getUser().getDisplayName()))
                .andExpect(jsonPath("user.email").value(msg2.getUser().getEmail()))
                .andExpect(jsonPath("votes").value(0))
                .andExpect(jsonPath("metadata").isMap())
                .andExpect(jsonPath("private").value(false))
                
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""),Matchers.containsString("rel=\"up\""))));
        
        callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(4)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        payload = callbackPayloadCapture.getValue(); //gets the result of the last call
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertTrue(analysis.getDate().after(created.getLastModified())); //the analysis of a newer version
        //Assert that tokens are NOT NULL but empty
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertTrue(analysis.getTokens().size() > 0);
        //Assert that Bern is extracted 2 times
        Assert.assertEquals(2, analysis.getTokens().stream().filter(t -> Objects.equals("Bern", t.getValue())).count());
        //Templates need also t0 be NOT NULL and NOT empty!
        Assert.assertNotNull(analysis.getTemplates());
        Assert.assertTrue(analysis.getTemplates().size() > 0);
        
        //now delete the last message
        this.mvc.perform(MockMvcRequestBuilders.delete("/conversation/" + created.getId().toHexString() + "/message/" + msg2.getId())
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(msg2)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(204))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"analyse\""),Matchers.containsString("rel=\"up\""))));
        
        Conversation retrieved = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.get("/conversation/" + created.getId())
                .header("X-Auth-Token", authToken.getToken())
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200))
              .andReturn().getResponse().getContentAsString(), Conversation.class);
        
        Assert.assertEquals(1, retrieved.getMessages().size());
        Assert.assertEquals(msg.getContent(), retrieved.getMessages().get(0).getContent());
        
        callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(callbackService,Mockito.timeout(60*1000).times(5)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
        Assert.assertEquals(callbackURI, callbackUriCapture.getValue().toString());
        payload = callbackPayloadCapture.getValue(); //gets the result of the last call
        Assert.assertEquals(HttpStatus.OK, payload.getHttpStatus());
        Assert.assertTrue(payload.getData() instanceof Analysis);
        analysis = (Analysis) payload.getData();
        Assert.assertEquals(created.getId(), analysis.getConversation());
        Assert.assertEquals(retrieved.getLastModified(), analysis.getDate()); //the analysis of a retrieved version
        //Assert that tokens are NOT NULL but empty
        Assert.assertNotNull(analysis.getTokens());
        Assert.assertTrue(analysis.getTokens().size() > 0);
        //Assert that Bern is extracted only once as the 2nd message was deleted
        Assert.assertEquals(1, analysis.getTokens().stream().filter(t -> Objects.equals("Bern", t.getValue())).count());
        //Templates need also t0 be NOT NULL and NOT empty!
        Assert.assertNotNull(analysis.getTemplates());
        Assert.assertTrue(analysis.getTemplates().size() > 0);
    }
    
    @Test
    public void testConversationSearch() throws Exception{
        String supportArea1 = "unit test";
        String supportArea2 = "integration test";
        Map<String,List<String>> supportAreaMessages = new HashMap<>();
        supportAreaMessages.put(supportArea1, Arrays.asList(
                "Wie kann in eine Komponente mit Abhängigkeiten testen?",
                "Wie definiere ich die jenigen Spring Komponenten welche ich für einen Test benötige",
                "Funktioniert das automatische Binden von Komponenten bei Unit Tests?"));
        
        supportAreaMessages.put(supportArea2, Arrays.asList(
                "Wird die Spring Umgebung mit allen Komponenten für jeden einzelnen Integration Test neu gestartet?",
                "Wie kann ich verhindern, dass Integration Tests bei jedem Build ausgeführt werden?"));
        
        List<Conversation> conversations = new LinkedList<>();
        int i = 0;
        for(Entry<String,List<String>> sa : supportAreaMessages.entrySet()){
            for(String content : sa.getValue()){
                String channel = "test-channel-" + i++;
                Conversation conversation = new Conversation();
                //conversation.setChannelId(channel);
                conversation.setOwner(client.getId());
                conversation.setMeta(new ConversationMeta());
                conversation.getMeta().setStatus(Status.New);
                conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, channel);
                conversation.getMeta().setProperty(ConversationMeta.PROP_SUPPORT_AREA, sa.getKey());
                conversation.getMeta().setProperty(ConversationMeta.PROP_TAGS, "testing");
                conversation.setContext(new Context());
                conversation.getContext().setDomain("test-domain");
                conversation.getContext().setContextType("text-context");
                conversation.getContext().setEnvironment("environment-test", "true");
                conversation.setUser(new User("alois.tester"));
                conversation.getUser().setDisplayName("Alois Tester");
                conversation.getUser().setEmail("alois.tester@test.org");
                Message msg = new Message(channel + "msg-1");
                msg.setContent(content);
                msg.setUser(conversation.getUser());
                msg.setOrigin(Origin.User);
                msg.setTime(new Date());
                conversation.getMessages().add(msg);
                conversations.add(conversationService.update(client, conversation));
            }
        }
        //now complete the conversations so that they get indexed
        conversations.forEach(c -> conversationService.completeConversation(c));
        
        //TODO: maybe we do not need to wait for indexing
        TimeUnit.SECONDS.sleep(5);
        
        String[] expectedResults = new String[]{
                conversations.get(0).getId().toHexString(),
                conversations.get(1).getId().toHexString(),
                conversations.get(2).getId().toHexString(),
                conversations.get(3).getId().toHexString()};
        
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/search")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                //.param("client", client.getId().toHexString()) //added based on the parsed auth-token
                .param("text", "Komponente"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("header").exists())
                .andExpect(jsonPath("header.params").exists())
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente^4")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente*^2")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("*Komponente*")))
                .andExpect(jsonPath("header.params.fq",Matchers.hasItem("owner:(" + client.getId().toHexString() + ")")))
                .andExpect(jsonPath("numFound").value(expectedResults.length))
                .andExpect(jsonPath("docs").isArray())
                .andExpect(jsonPath("docs[*].id",Matchers.containsInAnyOrder(expectedResults)));
        
        
        //search for Spring to rule out correct response by luck
        expectedResults = new String[]{
                conversations.get(1).getId().toHexString(),
                conversations.get(3).getId().toHexString()};

        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/search")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                //.param("client", client.getId().toHexString()) //added based on the parsed auth-token
                .param("text", "Spring"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("header").exists())
                .andExpect(jsonPath("header.params").exists())
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Spring^4")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Spring*^2")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("*Spring*")))
                .andExpect(jsonPath("header.params.fq",Matchers.hasItem("owner:(" + client.getId().toHexString() + ")")))
                .andExpect(jsonPath("numFound").value(expectedResults.length))
                .andExpect(jsonPath("docs").isArray())
                .andExpect(jsonPath("docs[*].id",Matchers.containsInAnyOrder(expectedResults)));

        //now add a custom filter query for the support area

        String[] expectedResultsSupportArea1 = new String[]{
                conversations.get(0).getId().toHexString(),
                conversations.get(1).getId().toHexString(),
                conversations.get(2).getId().toHexString()};
        
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/search")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                //.param("client", client.getId().toHexString()) //added based on the parsed auth-token
                .param("text", "Komponente")
                .param("fq", "meta_support_area:\""+supportArea1+"\""))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("header").exists())
                .andExpect(jsonPath("header.params").exists())
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente^4")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente*^2")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("*Komponente*")))
                .andExpect(jsonPath("header.params.fq",Matchers.hasItem("owner:(" + client.getId().toHexString() + ")")))
                .andExpect(jsonPath("numFound").value(expectedResultsSupportArea1.length))
                .andExpect(jsonPath("docs").isArray())
                .andExpect(jsonPath("docs[*].id",Matchers.containsInAnyOrder(expectedResultsSupportArea1)));
        
        String[] expectedResultsSupportArea2 = new String[]{
                conversations.get(3).getId().toHexString()};
        
        this.mvc.perform(MockMvcRequestBuilders.get("/conversation/search")
                .header("X-Auth-Token", authToken.getToken())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                //.param("client", client.getId().toHexString()) //added based on the parsed auth-token
                .param("text", "Komponente")
                .param("fq", "meta_support_area:\""+supportArea2+"\""))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("header").exists())
                .andExpect(jsonPath("header.params").exists())
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente^4")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("Komponente*^2")))
                .andExpect(jsonPath("header.params.q",Matchers.containsString("*Komponente*")))
                .andExpect(jsonPath("header.params.fq",Matchers.hasItem("owner:(" + client.getId().toHexString() + ")")))
                .andExpect(jsonPath("numFound").value(expectedResultsSupportArea2.length))
                .andExpect(jsonPath("docs").isArray())
                .andExpect(jsonPath("docs[*].id",Matchers.containsInAnyOrder(expectedResultsSupportArea2)));
        
    }
    @Test
    public void testQueryExecution() throws Exception {
        //As we use the related conversation query builder we need to create/complete some
        //conversation so that we do get some releated results
        String supportArea1 = "unit test";
        String supportArea2 = "integration test";
        Map<String,List<String>> supportAreaMessages = new HashMap<>();
        supportAreaMessages.put(supportArea1, Arrays.asList(
                "Wie kann in eine Komponente mit Abhängigkeiten testen?",
                "Wie definiere ich die jenigen Spring Komponenten welche ich für einen Test benötige",
                "Funktioniert das automatische Binden von Komponenten bei Unit Tests?",
                "Kann ich Komponenten auch im @BeforeClass verwenden?",
                "Werden Spring Komponenten für mehrere Unit Tests wiederverwendet?",
                "Wie kann ich Spring Webservices mit Unit Tests testen?"));
        
        supportAreaMessages.put(supportArea2, Arrays.asList(
                "Wird die Spring Umgebung mit allen Komponenten für jeden einzelnen Integration Test neu gestartet?",
                "Wie kann ich verhindern, dass Integration Tests bei jedem Build ausgeführt werden?"));
        
        List<Conversation> conversations = new LinkedList<>();
        int i = 0;
        for(Entry<String,List<String>> sa : supportAreaMessages.entrySet()){
            for(String content : sa.getValue()){
                String channel = "test-channel-" + i++;
                Conversation conversation = new Conversation();
                //conversation.setChannelId(channel);
                conversation.setOwner(client.getId());
                conversation.setMeta(new ConversationMeta());
                conversation.getMeta().setStatus(Status.New);
                conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, channel);
                conversation.getMeta().setProperty(ConversationMeta.PROP_SUPPORT_AREA, sa.getKey());
                conversation.getMeta().setProperty(ConversationMeta.PROP_TAGS, "testing");
                conversation.setContext(new Context());
                conversation.getContext().setDomain("test-domain");
                conversation.getContext().setContextType("text-context");
                conversation.getContext().setEnvironment("environment-test", "true");
                conversation.setUser(new User("alois.tester"));
                conversation.getUser().setDisplayName("Alois Tester");
                conversation.getUser().setEmail("alois.tester@test.org");
                Message msg = new Message(channel + "msg-1");
                msg.setContent(content);
                msg.setUser(conversation.getUser());
                msg.setOrigin(Origin.User);
                msg.setTime(new Date());
                conversation.getMessages().add(msg);
                conversations.add(conversationService.update(client, conversation));
            }
        }
        //now complete the conversations so that they get indexed
        conversations.forEach(c -> conversationService.completeConversation(c));
        
        //TODO: maybe we do not need to wait for indexing
        TimeUnit.SECONDS.sleep(5);
        
        //Now We need to create an additional conversation to get releated conversations
        Conversation conversation = new Conversation();
        conversation.setOwner(client.getId());
        conversation.setMeta(new ConversationMeta());
        conversation.getMeta().setStatus(Status.New);
        conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, "execution-test-channel");
        conversation.getMeta().setProperty(ConversationMeta.PROP_SUPPORT_AREA, supportArea1); //use the first for testing
        conversation.getMeta().setProperty(ConversationMeta.PROP_TAGS, "test");
        conversation.setContext(new Context());
        conversation.getContext().setDomain("test-domain");
        conversation.getContext().setContextType("text-context");
        conversation.getContext().setEnvironment("environment-test", "true");
        conversation.setUser(new User("alois.tester"));
        conversation.getUser().setDisplayName("Hubert Tester");
        conversation.getUser().setEmail("hubert.tester@test.org");
        Message msg = new Message("execution-test-channel");
        msg.setContent("Wie muss ich einen Spring Unit Test annotieren, damit ich Webservices testen kann?");
        msg.setUser(conversation.getUser());
        msg.setOrigin(Origin.User);
        msg.setTime(new Date());
        conversation.getMessages().add(msg);
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(201))
                .andReturn().getResponse();
        
        //now get the analysis
        String analyseLink = parseLinks(response.getHeaders("link")).get("analyse");
        Assert.assertNotNull(analyseLink);
        Analysis analysis = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.get(analyseLink)
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andReturn().getResponse().getContentAsString(), Analysis.class);
        //this just asserts that Analysis supports both parse and read
        Assert.assertNotNull(analysis);
        Assert.assertNotNull(analysis.getTemplates());
        AtomicInteger idx = new AtomicInteger(0);
        
        Entry<Integer, Template> templateEntry = analysis.getTemplates().stream()
            .map(t -> new ImmutablePair<Integer,Template>(idx.getAndIncrement(), t))
            .filter(p -> RELATED_CONVERSATION_TYPE.equals(p.getValue().getType()))
            .findAny().orElse(null);
        Assert.assertNotNull("missing expected template (type: " + RELATED_CONVERSATION_TYPE + ")", templateEntry);
        Query query = templateEntry.getValue().getQueries().stream()
            .filter(q -> q.getCreator().contains("queryBuilder:conversationmlt:"))
            .findAny().orElse(null);
        Assert.assertNotNull("missing expected query (creator: " + ConversationMltQueryBuilder.CREATOR_NAME + ")", query);
        
        String[] expectedResults = new String[]{
                conversations.get(5).getId().toHexString(),
                conversations.get(4).getId().toHexString(),
                conversations.get(2).getId().toHexString()};
        Conversation bestResult = conversations.get(5);
        Message bestMessage = bestResult.getMessages().get(0);
        
        //Now we can execute the query
        this.mvc.perform(MockMvcRequestBuilders.get(analyseLink + "/template/" + templateEntry.getKey() + "/result/" + query.getCreator())
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(jsonPath("numFound").value(5)) //5 of 6 have words in common
                .andExpect(jsonPath("start").value(0))
                .andExpect(jsonPath("pageSize").value(3))
                .andExpect(jsonPath("docs").isArray())
                .andExpect(jsonPath("docs[*].conversationId",Matchers.contains(expectedResults)))
                .andExpect(jsonPath("docs[0].replySuggestion").value(bestMessage.getContent()))
                .andExpect(jsonPath("docs[0].content").value(bestMessage.getContent()))
                .andExpect(jsonPath("docs[0].score").isNumber())
                .andExpect(jsonPath("docs[0].messageId").value(bestMessage.getId()))
                .andExpect(jsonPath("docs[0].userName").value(bestMessage.getUser().getDisplayName()))
                .andExpect(jsonPath("docs[0].messageIdx").value(0))
                .andExpect(jsonPath("docs[0].votes").value(0))
                .andExpect(jsonPath("docs[0].timestamp").value(bestMessage.getTime().getTime()))
                .andExpect(jsonPath("docs[0].creator").value(query.getCreator()));
        
    }
    
    private Map<String,String> parseLinks(List<String> headerValues){
        return headerValues.stream().map(LINK_HEADER_PATTERN::matcher)
            .filter(java.util.regex.Matcher::find)
            .collect(Collectors.toMap(m -> m.group(2), m -> m.group(1)));
        
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

    /**
     * Helper component that ensures that the correct Database version is set before the integration test runs
     */
    @Component
    public static class SmartiDbVersionInitializer {
        
        final MongoTemplate mongoTemplate;
        
        public SmartiDbVersionInitializer(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate;
        }
        
        @PostConstruct
        protected void initDatabaseVersion() {
            mongoTemplate.save(new DbVersion(SMARTI_DB_VERSION_ID).setVersion(EXPECTED_DB_VERSION), COLLECTION_NAME);
        }
    }
}
