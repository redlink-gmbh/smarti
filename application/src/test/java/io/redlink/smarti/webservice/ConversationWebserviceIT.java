package io.redlink.smarti.webservice;

import static java.nio.file.Files.createTempDirectory;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.redlink.smarti.model.*;
import io.redlink.smarti.repositories.AuthTokenRepository;
import io.redlink.smarti.services.AuthTokenService;
import io.redlink.smarti.services.AuthenticationService;

import org.apache.commons.collections4.CollectionUtils;
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
import org.mockito.BDDMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
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
import io.redlink.smarti.webservice.pojo.CallbackPayload;
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
        clientConfig = configService.createConfiguration(client);
        authToken = authTokenService.createAuthToken(client.getId(), "test");
    }
    
    @Test
    public void testGetConversation() throws Exception{
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
              .accept(MediaType.APPLICATION_JSON_VALUE))
              .andDo(MockMvcResultHandlers.print())
              .andExpect(MockMvcResultMatchers.status().is(200));
        
        //Now create a 2nd client
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        Configuration client2Config = configService.createConfiguration(client2);
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
        
        //Now create a 2nd client
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        Configuration client2Config = configService.createConfiguration(client2);
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
        Configuration client2Config = configService.createConfiguration(client2);
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

    }
    
    @Test
    public void testListConversations() throws Exception{
        Client client2 = new Client();
        client2.setName("test-client-2");
        client2.setDescription("An other Client created for testing");
        client2.setDefaultClient(false);
        client2 = clientService.save(client2);
        Configuration client2Config = configService.createConfiguration(client2);
        AuthToken authToken2 = authTokenService.createAuthToken(client2.getId(), "test");

        Map<String,Conversation> client1Conversations = new HashMap<>();
        Map<String,Conversation> client2Conversations = new HashMap<>();
        
        for(int i = 0 ; i < 20; i++){
            Conversation conversation = new Conversation();
            conversation.setChannelId("test-channel-" + i%2);
            conversation.setOwner(i%2 == 0 ? client.getId() : client2.getId());
            conversation.setMeta(new ConversationMeta());
            conversation.getMeta().setStatus(Status.New);
            conversation.getMeta().setProperty(ConversationMeta.PROP_CHANNEL_ID, conversation.getChannelId());
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
                .param("clientId", client2.getId().toHexString())
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);
        
        String callbackURI = "http://www.example.org/smarti/callback/test";
        
        Conversation created = objectMapper.readValue(this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .param("callback", callbackURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.header().stringValues("link", (Matcher)Matchers.containsInAnyOrder(
                        Matchers.containsString("rel=\"self\""),Matchers.containsString("rel=\"analyse\""))))
                .andReturn().getResponse().getContentAsString(),Conversation.class);
       
        //now wait for the callback!
        ArgumentCaptor<CallbackPayload> callbackPayloadCapture = ArgumentCaptor.forClass(CallbackPayload.class);
        ArgumentCaptor<URI> callbackUriCapture = ArgumentCaptor.forClass(URI.class);
        BDDMockito.verify(callbackService,BDDMockito.timeout(10*1000).times(1)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
        
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
    public void testCreateConversationAnalysisRequest() throws Exception{
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
        String conversationJson = objectMapper.writerFor(Conversation.class).writeValueAsString(conversation);

        MockHttpServletResponse response = this.mvc.perform(MockMvcRequestBuilders.post("/conversation")
                .header("X-Auth-Token", authToken.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(conversationJson))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
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
    }
    
    @Test
    public void testCreateConversationAndAddMessage() throws Exception{
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
                .andExpect(MockMvcResultMatchers.status().is(200))
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
                .andExpect(MockMvcResultMatchers.status().is(200))
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
        BDDMockito.verify(callbackService,BDDMockito.timeout(10*1000).times(2)).execute(callbackUriCapture.capture(), callbackPayloadCapture.capture());
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
}
