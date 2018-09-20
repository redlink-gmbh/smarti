package io.redlink.smarti.repositories;

import java.util.Arrays;
import java.util.Date;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta.Status;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Message.Origin;
import io.redlink.smarti.model.User;
import io.redlink.smarti.test.SpringServiceTest;

@ContextConfiguration(classes={ConversationRepoListener.class})
@EnableMongoRepositories(basePackageClasses={ConversationRepository.class})
@EnableAutoConfiguration
public class ConversationRepositoryTest extends SpringServiceTest {

    
    @Autowired
    private ConversationRepository conversationRepo;
    
    
    @Test
    public void testMessageLimit() throws Exception {
        ObjectId owner = new ObjectId();
        Conversation conv = new Conversation();
        conv.setOwner(owner);

        conv.getContext().setDomain("test");
        conv.getContext().setContextType("test");
        conv.getContext().setEnvironment("test", "test");

        User user0 = new User();
        user0.setDisplayName("Test Dummy");
        user0.setEmail("test.dummy@test.org");
        user0.setHomeTown("Testhausen");
        conv.setUser(user0);
        
        User user1 = new User();
        user1.setDisplayName("Maria Testament");
        user1.setEmail("maria.testament@test.org");
        user1.setHomeTown("Antesten");
        
        conv.getMeta().setStatus(Status.New);
        conv.getMeta().setProperty("test", Arrays.asList("test1","test2","test3"));
        
        for(int i=0; i < 55 ; i++){
            Message message = new Message("msg-"+i);
            message.setOrigin(Origin.User);
            message.setUser(i%2 == 0 ? user0 : user1);
            message.setTime(new Date());
            message.setContent("This is the " + (i + 1) + "message of this conversation");
            conv.getMessages().add(message);
        }
        
        //now save the conversation in the Repository
        Conversation created = conversationRepo.save(conv);
        Assert.assertNotNull(created.getId());
        //as the message limit is set to 50 we expect the first 5 Messages to be sliced
        Assert.assertEquals(50, created.getMessages().size());
        Assert.assertEquals("msg-5", conv.getMessages().get(0).getId());
        Assert.assertEquals("msg-54", created.getMessages().get(created.getMessages().size() - 1).getId());
        
        //now lets append a message
        Message appended = new Message("appended-0");
        appended.setOrigin(Origin.User);
        appended.setUser(user1);
        appended.setContent("This is the first appended Message");
        Conversation updated = conversationRepo.appendMessage(created, appended);
        Assert.assertEquals(50,  updated.getMessages().size());
        Assert.assertEquals("msg-6", updated.getMessages().get(0).getId());
        Assert.assertEquals("appended-0", updated.getMessages().get(updated.getMessages().size() - 1).getId());
        
        //lets delete a message and append an other
        Assert.assertTrue(conversationRepo.deleteMessage(updated.getId(), "msg-10"));
        updated = conversationRepo.findOne(updated.getId());
        Assert.assertEquals(49, updated.getMessages().size());
        appended = new Message("appended-1");
        appended.setOrigin(Origin.User);
        appended.setUser(user0);
        appended.setContent("This is the second appended Message");
        updated = conversationRepo.appendMessage(updated, appended);
        Assert.assertEquals(50,  updated.getMessages().size());
        Assert.assertEquals("msg-6", updated.getMessages().get(0).getId());
        Assert.assertEquals("appended-1", updated.getMessages().get(updated.getMessages().size() - 1).getId());
        
    }
    
}
