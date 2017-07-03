/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.User;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.webservice.pojo.RocketEvent;
import io.redlink.smarti.webservice.pojo.SmartiUpdatePing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ConnectionBackoffStrategy;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Webhook-Endpoint for rocket.chat
 */
@CrossOrigin
@RestController
@RequestMapping(value = "rocket",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("rocket")
public class RocketChatEndpoint {

    public static final String ROCKET_CHAT = "rocket.chat";
    private Logger log = LoggerFactory.getLogger(RocketChatEndpoint.class);

    @Value("${smarti.debug:false}")
    private boolean debug = false;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ConversationService conversationService;

    private final HttpClientBuilder httpClientBuilder;

    public RocketChatEndpoint() {
        httpClientBuilder = HttpClientBuilder.create()
                .setRetryHandler((exception, executionCount, context) -> executionCount < 3)
                .setConnectionBackoffStrategy(new ConnectionBackoffStrategy() {
                    @Override
                    public boolean shouldBackoff(Throwable t) {
                        return t instanceof IOException;
                    }

                    @Override
                    public boolean shouldBackoff(HttpResponse resp) {
                        return false;
                    }
                })
                .setUserAgent("Smarti/0.0 Rocket.Chat-Endpoint/0.1");
    }


    @ApiOperation("webhook-target for " + ROCKET_CHAT)
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK")
    })
    @RequestMapping(value = "{clientId:.*}", method = RequestMethod.POST)
    public ResponseEntity<?> onRocketEvent(@PathVariable("clientId") String clientId,
                                           @RequestBody RocketEvent payload) {
        log.debug("{}: {}", clientId, payload);

        final String channelId = createChannelId(clientId, payload.getChannelId());
        Conversation conversation = storeService.getCurrentConversationByChannelId(channelId, () -> {
            Conversation newConversation = new Conversation();
            newConversation.getContext().setContextType(ROCKET_CHAT);
            newConversation.getContext().setDomain(clientId);
            newConversation.getContext().setEnvironment("channel", payload.getChannelName());
            newConversation.getContext().setEnvironment("channel_id", payload.getChannelId());
            newConversation.getContext().setEnvironment("token", payload.getToken());
            return newConversation;
        });

        final Message message = new Message();
        message.setId(payload.getMessageId());
        message.setContent(payload.getText());
        message.setTime(payload.getTimestamp());
        message.setOrigin(payload.isBot() ? Message.Origin.Agent : Message.Origin.User);

        // TODO: Use a UserService to actually *store* the users
        final User user = new User(payload.getUserId());
        user.setDisplayName(payload.getUserName());
        message.setUser(user);

        if(StringUtils.isNoneBlank(payload.getTriggerWord())){
            message.getMetadata().put("trigger_word", payload.getTriggerWord());
        }
        if (payload.isBot()) {
            message.getMetadata().put("bot_id", payload.getBot().getIdentifier());
        }

        conversation = conversationService.appendMessage(conversation, message, (c) -> notifyRocketChat(payload.getCallbackUrl(), c, payload.getToken()));

        return ResponseEntity.ok().build();
    }

    private void notifyRocketChat(String callbackUrl, Conversation conversation, String token) {
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            final HttpPost post = new HttpPost(callbackUrl);
            post.setEntity(new StringEntity(
                    toJsonString(new SmartiUpdatePing(conversation.getId(), conversation.getContext().getEnvironment("channel_id"), token)),
                    ContentType.APPLICATION_JSON
            ));
            httpClient.execute(post, response -> null);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Callback to Rocket.Chat <{}> failed: {}", callbackUrl, e.getMessage(), e);
            } else {
                log.error("Callback to Rocket.Chat <{}> failed: {}", callbackUrl, e.getMessage());
            }
        }
    }

    private String toJsonString(SmartiUpdatePing smartiUpdatePing) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(smartiUpdatePing);
    }

    /**
     * Called by rocket.chat plugins to get the conversationId for the clientId and channelId known to the plugin.
     * The returned conversationID can later be used for calls to the {@link ConversationWebservice}
     * @param clientId the client id
     * @param channelId the channelId
     * @return a <code>200</code> with the conversation id as payload or a <code>404</code> if no conversation is
     * active for the parsed parameters.
     */
    @ApiOperation(value = "retrieve a conversation ID for a channel and client id", produces=MimeTypeUtils.TEXT_PLAIN_VALUE)
    @RequestMapping(value = "{clientId}/{channelId}/conversationid", method = RequestMethod.GET,
        produces=MimeTypeUtils.TEXT_PLAIN_VALUE, consumes=MimeTypeUtils.ALL_VALUE)
    public ResponseEntity<?> getConversation(
            @PathVariable(value="clientId") String clientId,
            @PathVariable(value="channelId") String channelId) {
        Conversation conversation = storeService.getCurrentConversationByChannelId(createChannelId(clientId, channelId),() -> null); //do not create new conversations
        if (conversation == null || conversation.getId() == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getId().toHexString());
        }
    }

    public String createChannelId(String clientId, String roomId) {
        Preconditions.checkNotNull(clientId, "Missing parameter <clientId>");
        Preconditions.checkNotNull(roomId, "Missing parameter <roomId>");

        return String.format("%s/%s/%s", ROCKET_CHAT, clientId, roomId);
    }

}
