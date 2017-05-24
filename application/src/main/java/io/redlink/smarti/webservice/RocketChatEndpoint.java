/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.User;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.webservice.pojo.RocketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Webhook-Endpoint for rocket.chat
 */
@CrossOrigin
@RestController
@RequestMapping(value = "rocket",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
public class RocketChatEndpoint {

    private Logger log = LoggerFactory.getLogger(RocketChatEndpoint.class);

    @Autowired
    private StoreService storeService;

    @Autowired
    private ConversationService conversationService;

    @RequestMapping(value = "{clientId}", method = RequestMethod.POST)
    public ResponseEntity<?> onRocketEvent(@PathVariable("clientId") String clientId,
                                           @RequestBody RocketEvent payload) {
        log.debug("{}: {}", clientId, payload);

        final String channelId = createChannelId(clientId, payload.getChannelId());
        final Conversation conversation = storeService.getConversationByChannelId(channelId);

        final Message message = new Message();
        message.setContent(payload.getText());
        message.setTime(payload.getTimestamp());
        message.setOrigin(payload.isBot() ? Message.Origin.Agent : Message.Origin.User);

        // TODO: Use a UserServcice to actually *store* the users
        final User user = new User(payload.getUserId());
        user.setDisplayName(payload.getUserName());
        message.setUser(user);

        final Map<String,String> meta = new HashMap<>();
        meta.put("message_id", payload.getMessageId());
        meta.put("trigger_word", payload.getTriggerWord());
        message.setMetadata(meta);

        conversationService.appendMessage(conversation, message);

        return ResponseEntity.accepted().build();
    }

    public String createChannelId(String clientId, String roomId) {
        return String.format("rocket.chat/%s/%s", clientId, roomId);
    }

}
