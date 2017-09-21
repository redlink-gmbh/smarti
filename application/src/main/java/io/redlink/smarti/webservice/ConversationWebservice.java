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

import com.google.common.collect.ImmutableMap;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.webservice.pojo.Projection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 *
 */
@SuppressWarnings("unused")
@CrossOrigin
@RestController
@RequestMapping(value = "conversation",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation")
public class ConversationWebservice {

    private final AsyncExecutionService asyncExecutionService;
    private final ConversationService conversationService;
    private final ClientService clientService;

    @Autowired
    public ConversationWebservice(AsyncExecutionService asyncExecutionService, ConversationService conversationService, ClientService clientService) {
        this.asyncExecutionService = asyncExecutionService;
        this.conversationService = conversationService;
        this.clientService = clientService;
    }

    @ApiOperation(value = "list conversations", response = Conversation.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public Page<Conversation> listConversations(
            @RequestParam(value = "clientId", required = false) ObjectId owner,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        //TODO: check authentication

        return conversationService.listConversations(owner, page, pageSize);

    }

    @ApiOperation(value = "create a conversation", response = Conversation.class)
    @ApiResponses(
            @ApiResponse(code = 201, message = "conversation created")
    )
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Conversation> createConversation(
            UriComponentsBuilder uriBuilder,
            @RequestBody(required = false) Conversation conversation,
            @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
            ) {
        //TODO: check authentication
        conversation = Optional.ofNullable(conversation).orElseGet(Conversation::new);
        // Create a new Conversation -> id must be null
        conversation.setId(null);
        //TODO: set the owner of the conversation based on the current auth
        //FIXME for now the owner needs to be parsed with the conversation!
        final Client client = clientService
                .get(ObjectUtils.firstNonNull(conversation.getOwner(), conversation.getClientId()));
        if(client == null){
            throw new IllegalStateException("Owner for new conversation not provided!");
        }

        final Conversation stored = conversationService.update(client, conversation, false, null);

        return asyncExecutionService.execute(
                () -> conversationService.update(client, stored, true, null),
                HttpStatus.CREATED, callback, stored.getId(), buildConversationURI(uriBuilder, stored.getId()));

    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<Conversation> getConversation(
            UriComponentsBuilder uriBuilder,
            @PathVariable("id") ObjectId conversationId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO get the client for the authenticated user
        final Conversation conversation = conversationService.getConversation(null, conversationId);

        //TODO: check that the client has the right to access the conversation

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation);
        }
    }

    @ApiOperation(value = "delete a conversation", code = 204)
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteConversation(
            @PathVariable("id") ObjectId conversationId
    ) {
        //TODO get the client for the authenticated user

        //TODO: check that the client has the right to access the conversation
        final Conversation conversation = conversationService.deleteConversation(conversationId);

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @ApiOperation(value = "update/modify a specific field", response = Conversation.class)
    @RequestMapping(value = "{id}/{field:.*}", method = RequestMethod.PUT)
    public ResponseEntity<Conversation> modifyConversationField(
            UriComponentsBuilder uriBuilder,
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("field") String field,
            @RequestBody Object data,
            @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: Check Authentication

        if (conversationService.exists(conversationId)) {
            return asyncExecutionService.execute(
                    () -> conversationService.updateConversationField(conversationId, field, data),
                    HttpStatus.OK,
                    callback,
                    conversationId, buildConversationURI(uriBuilder, conversationId));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "list the messages in a conversation", response = Message.class, responseContainer = "List")
    @RequestMapping(value = "{id}/message", method = RequestMethod.GET)
    public ResponseEntity<List<Message>> listMessages(
            @PathVariable("id") ObjectId conversationId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO: check authentication

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getMessages());
        }
    }

    @ApiOperation(value = "create a new message", response = Message.class)
    @ApiResponses(
            @ApiResponse(code = 201, message = "message created")
    )
    @RequestMapping(value = "{id}/message", method = RequestMethod.POST)
    public ResponseEntity<Message> appendMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("id") ObjectId conversationId,
            @RequestBody Message message,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO: get the Client for the currently authenticated user
        final Conversation conversation = conversationService.getConversation(null, conversationId); //TODO: pass the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        //TODO: check that the authenticated user has rights to update messages of this client
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }

        //TODO: delegate id-generation to database
        if (StringUtils.isBlank(message.getId())) {
            message.setId(UUID.randomUUID().toString());
        }
        return asyncExecutionService.execute(
                () -> {
                    final List<Message> theMessages = conversationService.appendMessage(client, conversation, message).getMessages();
                    return theMessages.get(theMessages.size()-1);
                },
                HttpStatus.OK,
                callback, message.getId(), buildMessageURI(uriBuilder, conversationId, message.getId()));
    }

    @ApiOperation(value = "retrieve a message", response = Message.class)
    @RequestMapping(value = "{id}/message/{msgId}", method = RequestMethod.GET)
    public void getMessage(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("msgId") ObjectId messageId
    ) {}

    @ApiOperation(value = "update/replace a message", response = Message.class)
    @RequestMapping(value = "{id}/message/{msgId}", method = RequestMethod.PUT)
    public void updateMessage(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("msgId") ObjectId messageId,
            @RequestBody(required = false) Message message,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {}

    @ApiOperation(value = "delete a message", code = 204)
    @RequestMapping(value = "{id}/message/{msgId}", method = RequestMethod.DELETE)
    public void deleteMessage(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("msgId") ObjectId messageId
    ) {}

    @ApiOperation(value = "update/modify a specific filed of the message", response = Message.class)
    @RequestMapping(value = "{id}/message/{msgId}/{field}", method = RequestMethod.PUT)
    public void modifyMessageField(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("msgId") ObjectId messageId,
            @PathVariable("field") String field,
            @RequestBody Object data,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {}

    @ApiOperation(value = "get the extracted tokes in the conversation", response = Analysis.class)
    @RequestMapping(value = "{id}/analysis", method = RequestMethod.GET)
    public void getAnalysis(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "re-run analysis based on updated tokens/slot-assignments", response = Analysis.class)
    @RequestMapping(value = "{id}/analysis", method = RequestMethod.POST)
    public void rerunAnalysis(
            @PathVariable("id") ObjectId conversationId,
            @RequestBody Analysis updatedAnalysis,
            @RequestParam(value = "callback", required = false) URI callback
    ) {}


    @ApiOperation(value = "get the extracted tokes in the conversation", response = Token.class, responseContainer = "List")
    @RequestMapping(value = "{id}/analysis/token", method = RequestMethod.GET)
    public void getTokens(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{id}/analysis/template", method = RequestMethod.GET)
    public void getTemplates(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "get a query template", response = Template.class)
    @RequestMapping(value = "{id}/analysis/template/{templateIdx}", method = RequestMethod.GET)
    public void getTemplate(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx
    ) {}

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = Result.class, responseContainer = "List")
    @RequestMapping(value = "{id}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.GET)
    public void getResults(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") int creator
    ) {}

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{id}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.POST)
    public void rerunResults(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") int creator,
            @RequestBody Analysis updatedAnalysis,
            @RequestParam(value = "callback", required = false) URI callback
    ) {}

    private URI buildConversationURI(UriComponentsBuilder builder, ObjectId conversationId) {
        return builder
                .pathSegment("conversation", "{conversationId}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId
                ))
                .toUri();
    }

    private URI buildMessageURI(UriComponentsBuilder builder, ObjectId conversationId, String messageId) {
        return builder
                .pathSegment("conversation", "{conversationId}", "message", "{messageId}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "messageId", messageId
                ))
                .toUri();
    }
}
