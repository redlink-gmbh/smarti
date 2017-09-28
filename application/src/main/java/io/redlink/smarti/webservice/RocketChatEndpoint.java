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
package io.redlink.smarti.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.redlink.smarti.model.*;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.RocketEvent;
import io.redlink.smarti.webservice.pojo.SmartiUpdatePing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
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

    //@Autowired
    //private StoreService storeService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ConversationService conversationService;

    protected final HttpClientBuilder httpClientBuilder;

    public RocketChatEndpoint(
            @Value("${rocketchat.proxy.hostname:}") String proxyHostname,
            @Value("${rocketchat.proxy.port:80}") int proxyPort,
            @Value("${rocketchat.proxy.scheme:http}") String proxyScheme
    ) {

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

        if(StringUtils.isNotBlank(proxyHostname)) {
            httpClientBuilder.setProxy(new HttpHost(proxyHostname, proxyPort, proxyScheme));
        }
    }


    @ApiOperation("webhook-target for " + ROCKET_CHAT)
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK")
    })
    @RequestMapping(value = "{clientId:.*}", method = RequestMethod.POST)
    public ResponseEntity<?> onRocketEvent(@PathVariable("clientId") String clientName,
                                           @RequestBody RocketEvent payload) {
        log.debug("{}: {}", clientName, payload);

        Client client = clientService.getByName(clientName);
        if(client == null) { //TODO: make client generation configurable
            client = new Client();
            client.setName(clientName);
            client = clientService.save(client);
        }

        final String channelId = createChannelId(client, payload.getChannelId());
        Conversation conversation = conversationService.getCurrentConversationByChannelId(client, channelId, () -> {
            Conversation newConversation = new Conversation();
            //newConversation.setOwner(clientId); -> set by the method
            newConversation.getContext().setContextType(ROCKET_CHAT);
            newConversation.getContext().setDomain(clientName);
            newConversation.getContext().setEnvironment(Context.ENV_CHANNEL_NAME, payload.getChannelName());
            newConversation.getContext().setEnvironment(Context.ENV_CHANNEL_ID, payload.getChannelId());
            newConversation.getContext().setEnvironment(Context.ENV_TOKEN, payload.getToken());
            newConversation.getContext().setEnvironment(Context.ENV_SUPPORT_AREA, payload.getSupportArea());
            return newConversation;
        });
        if(!conversation.getChannelId().equals(channelId)){
            return ResponseEntities.conflict("clientId does not match for existing Conversation[id:" + conversation.getId()+ "]");
        }

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

        conversation = conversationService.appendMessage(client, conversation, message, (c) -> notifyRocketChat(payload.getCallbackUrl(), c, payload.getToken()));

        return ResponseEntity.ok().build();
    }

    private void notifyRocketChat(String callbackUrl, Conversation conversation, String token) {
        if (StringUtils.isBlank(callbackUrl)) return;

        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            final HttpPost post = new HttpPost(callbackUrl);
            final MultiValueMap<String, String> env = CollectionUtils.toMultiValueMap(conversation.getContext().getEnvironment());
            post.setEntity(new StringEntity(
                    toJsonString(new SmartiUpdatePing(conversation.getId(), env.getFirst(Context.ENV_CHANNEL_ID), token)),
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
     * @param clientName the client id
     * @param channelId the channelId
     * @return a <code>200</code> with the conversation id as payload or a <code>404</code> if no conversation is
     * active for the parsed parameters.
     */
    @ApiOperation(value = "retrieve a conversation ID for a channel and client id", produces=MimeTypeUtils.TEXT_PLAIN_VALUE)
    @RequestMapping(value = "{clientId}/{channelId}/conversationid", method = RequestMethod.GET,
        produces=MimeTypeUtils.TEXT_PLAIN_VALUE, consumes=MimeTypeUtils.ALL_VALUE)
    public ResponseEntity<?> getConversation(
            @PathVariable(value="clientId") String clientName,
            @PathVariable(value="channelId") String channelId) {
        Client client = clientService.getByName(clientName);
        if(client == null){
            return ResponseEntity.notFound().build();
        }
        Conversation conversation = conversationService.getCurrentConversationByChannelId(
                client, createChannelId(client, channelId),() -> null); //do not create new conversations
        if (conversation == null || conversation.getId() == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getId().toHexString());
        }
    }

    public String createChannelId(Client client, String roomId) {
        Preconditions.checkNotNull(client, "Missing parameter <client>");
        Preconditions.checkNotNull(client.getId(), "Invalid parameter <client>");
        Preconditions.checkNotNull(roomId, "Missing parameter <roomId>");

        return String.format("%s/%s/%s", ROCKET_CHAT, client.getId(), roomId);
    }

}
