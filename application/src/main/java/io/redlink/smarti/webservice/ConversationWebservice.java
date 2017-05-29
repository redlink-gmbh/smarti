/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.utils.ModelUtils;
import io.redlink.smarti.utils.ResponseEntities;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 *
 */
@CrossOrigin
@RestController
@RequestMapping(value = "conversation",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation")
public class ConversationWebservice {

    @Autowired
    private StoreService storeService;

    @Autowired
    private ConversationService conversationService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createConversation() {
        return ResponseEntity.ok(storeService.store(new Conversation()));
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getConversation(@PathVariable("id") String id) {
        final Conversation conversation = storeService.get(ModelUtils.parseObjectId(id, "id"));

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateConversation(@PathVariable("id") String id,
                                                @RequestBody Conversation conversation) {
        if (Objects.equals(conversation.getId(), id)) {
            return ResponseEntity.ok(storeService.store(conversation));
        } else {
            return ResponseEntities.badRequest("request-url and content do not match");
        }
    }

    @RequestMapping(value = "{id}/message", method = RequestMethod.POST)
    public ResponseEntity<?> addMessage(@PathVariable("id") String id,
                                        @RequestBody Message message) {
        final Conversation conversation = storeService.get(ModelUtils.parseObjectId(id, "id"));
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(conversationService.appendMessage(conversation, message));
    }

    @RequestMapping(value = "{id}/analysis", method = RequestMethod.GET)
    public ResponseEntity<?> prepare(@PathVariable("id") String id) {
        final Conversation conversation = storeService.get(ModelUtils.parseObjectId(id, "id"));

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getTokens());
        }
    }

    @RequestMapping(value = "{id}/intent", method = RequestMethod.GET)
    public ResponseEntity<?> query(@PathVariable("id") String id) {
        final Conversation conversation = storeService.get(ModelUtils.parseObjectId(id, "id"));

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getQueryTemplates());
        }
    }

    @RequestMapping(value = "{id}/intent/{intent}/{creator}", method = RequestMethod.GET)
    public ResponseEntity<?> getResults(@PathVariable("id") String id,
                                        @PathVariable("intent") String intent,
                                        @PathVariable("creator") String creator) {
        // TODO: Implement this
        return ResponseEntities.notImplemented();
    }

    @RequestMapping(value = "{id}/publish", method = RequestMethod.POST)
    public ResponseEntity<?> complete(@PathVariable("id") String id) {
        final Conversation conversation = storeService.get(ModelUtils.parseObjectId(id, "id"));

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
            return ResponseEntity.ok(storeService.store(conversation));
        }
    }
}
