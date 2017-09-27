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
import io.swagger.annotations.*;
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

import java.io.IOException;
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

    private static final String API_PARAM_CALLBACK = "URI where to POST the result (triggers async processing)";
    private static final String API_ASYNC_NOTE = " If a 'callback' is provided, the request will be processed async.";
    private static final String EDITABLE_CONVERSATION_FIELDS = "meta.status, meta.tags, meta.feedback, context.contextType, context.environmentType, context.domain, context.environment.*";
    private static final String EDITABLE_MESSAGE_FIELDS = "time, origin, content, private, votes, metadata.*";

    private final AsyncExecutionService asyncExecutionService;
    private final ConversationService conversationService;
    private final ClientService clientService;

    @Autowired
    public ConversationWebservice(AsyncExecutionService asyncExecutionService, ConversationService conversationService, ClientService clientService) {
        this.asyncExecutionService = asyncExecutionService;
        this.conversationService = conversationService;
        this.clientService = clientService;
    }

    @ApiOperation(value = "list conversations", response = Page.class)
    @RequestMapping(method = RequestMethod.GET)
    public Page<Conversation> listConversations(
            @RequestParam(value = "clientId", required = false) ObjectId owner,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        //TODO: check authentication, limit clientId to value(s) the user has access to

        return conversationService.listConversations(owner, page, pageSize);

    }

    @ApiOperation(value = "create a conversation",
            code = 201, response = Conversation.class,
            notes = "Create a new Conversation." + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "conversation created (sync)", response = Conversation.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Conversation> createConversation(
            UriComponentsBuilder uriBuilder,
            @RequestBody(required = false) Conversation conversation,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
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

        final Conversation stored = conversationService.update(client, conversation, false);

        return asyncExecutionService.execute(
                () -> conversationService.update(client, stored, true),
                HttpStatus.CREATED, callback, stored.getId(), buildConversationURI(uriBuilder, stored.getId()));

    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.GET)
    public ResponseEntity<Conversation> getConversation(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO get the client for the authenticated user
        final Client client = null;
        final Conversation conversation = conversationService.getConversation(client, conversationId);

        //TODO: check that the client has the right to access the conversation

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation);
        }
    }

    @ApiOperation(value = "delete a conversation", code = 204)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteConversation(
            @PathVariable("conversationId") ObjectId conversationId
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

    @ApiOperation(value = "update/modify a specific field", response = Conversation.class,
            notes = "Update a single property in the Conversation." + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "field updated (sync)", response = Conversation.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(value = "{conversationId}/{field:.*}", method = RequestMethod.PUT)
    public ResponseEntity<Conversation> modifyConversationField(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(value = "the field to update", required = true, allowableValues = EDITABLE_CONVERSATION_FIELDS) @PathVariable("field") String field,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
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

    @ApiOperation(value = "list the messages in a conversation", response = Message.class, responseContainer = "List",
            notes = "retrieves all messages in the accessed conversation")
    @RequestMapping(value = "{conversationId}/message", method = RequestMethod.GET)
    public ResponseEntity<List<Message>> listMessages(
            @PathVariable("conversationId") ObjectId conversationId,
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

    @ApiOperation(value = "create a new message", response = Message.class,
            notes = "this appends the provided message to the conversation. It is the responsibility of the client to ensure" +
                    "that a messageId passed in is unique. It the client cannot guarantee that, it MUST leave the messageId " +
                    "empty/null." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "message created"),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(value = "{conversationId}/message", method = RequestMethod.POST)
    public ResponseEntity<Message> appendMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Message message,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false)URI callback,
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
                    conversationService.appendMessage(client, conversation, message);
                    return conversationService.getMessage(conversation.getId(), message.getId());
                },
                HttpStatus.OK,
                callback, message.getId(), buildMessageURI(uriBuilder, conversationId, message.getId()));
    }

    @ApiOperation(value = "retrieve a message", response = Message.class)
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.GET)
    public ResponseEntity<Message> getMessage(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        // TODO: Check authentication

        final Message message = conversationService.getMessage(conversationId, messageId);
        if (message == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(message);
        }

    }

    @ApiOperation(value = "update/replace a message", response = Message.class,
            notes = "fully replace a message." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "message updated (sync)", response = Conversation.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.PUT)
    public ResponseEntity<Message> updateMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestBody Message message,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: Check authentication / clientId

        //make sure the message-id is the addressed one
        message.setId(messageId);

        return asyncExecutionService.execute(() -> {
                    conversationService.updateMessage(conversationId, message);
                    return conversationService.getMessage(conversationId, message.getId());
                },
                HttpStatus.OK, callback, messageId, buildMessageURI(uriBuilder, conversationId, messageId));
    }

    @ApiOperation(value = "delete a message", code = 204,
            notes = "delete a message and re-run analysis based on the new conversation." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 204, message = "message deleted (sync)", response = Conversation.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.DELETE)
    public ResponseEntity<Conversation> deleteMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false)URI callback
    ) {
        // TODO: Check Authentication / clientId

        if (!conversationService.exists(conversationId, messageId)) {
            return ResponseEntity.notFound().build();
        }

        return asyncExecutionService.execute(
                () -> {
                    final boolean b = conversationService.deleteMessage(conversationId, messageId);
                    if (b) return conversationService.getConversation(conversationId);
                    return null;
                },
                callback,
                (c) -> ResponseEntity.noContent().build(),
                conversationId, buildConversationURI(uriBuilder, conversationId));
    }

    
    @ApiOperation(value = "update/modify a specific filed of the message", response = Message.class,
            notes = "Update a single property in the Message." + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "field updated (sync)", response = Message.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}/{field}", method = RequestMethod.PUT)
    public ResponseEntity<Message> modifyMessageField(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(value = "the field to update", required = true, allowableValues = EDITABLE_MESSAGE_FIELDS) @PathVariable("field") String field,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: check Authentication / clientId

        if (conversationService.exists(conversationId, messageId)) {
            return asyncExecutionService.execute(() ->
                            conversationService.updateMessageField(conversationId, messageId, field, data),
                    HttpStatus.OK,
                    callback,
                    messageId, buildMessageURI(uriBuilder, conversationId, messageId));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "get the analysis-results of the conversation", response = Analysis.class,
            notes = "retrieve the analysis for this conversation.")
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.GET)
    public ResponseEntity<Analysis> getAnalysis(
            @PathVariable("conversationId") ObjectId conversationId
    ) {
        // TODO: check Authentication / clientId

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getAnalysis());
        }
    }

    @ApiOperation(value = "re-run analysis based on updated tokens/slot-assignments", response = Analysis.class,
    notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.POST)
    public ResponseEntity<Analysis> rerunAnalysis(
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: check Authentication / clientId

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }


    @ApiOperation(value = "get the extracted tokes in the conversation", response = Token.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/token", method = RequestMethod.GET)
    public ResponseEntity<List<Token>> getTokens(
            @PathVariable("conversationId") ObjectId conversationId
    ) {
        // TODO: check Authentication / clientId

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getAnalysis().getTokens());
        }

    }

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/template", method = RequestMethod.GET)
    public ResponseEntity<List<Template>> getTemplates(
            @PathVariable("conversationId") ObjectId conversationId
    ) {
        // TODO: check Authentication / clientId

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getAnalysis().getTemplates());
        }

    }

    @ApiOperation(value = "get a query template", response = Template.class)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}", method = RequestMethod.GET)
    public ResponseEntity<Template> getTemplate(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx
    ) {
        // TODO: check Authentication / clientId

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            final List<Template> templates = conversation.getAnalysis().getTemplates();
            if (templateIdx < 0) {
                return ResponseEntity.badRequest().build();
            } else if (templateIdx < templates.size()) {
                return ResponseEntity.ok(templates.get(templateIdx));
            } else {
                return ResponseEntity.notFound().build();
            }
        }

    }

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = Result.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.GET)
    public ResponseEntity<? extends List<? extends Result>> getResults(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator
    ) {
        // TODO: check Authentication / clientId
        final Client client = null;
        final Conversation conversation = conversationService.getConversation(client, conversationId);
        if (conversation == null) return ResponseEntity.notFound().build();

        final List<Template> templates = conversation.getAnalysis().getTemplates();
        final Template template;
        if (templateIdx < 0) return ResponseEntity.badRequest().build();
        else if (templateIdx < templates.size()) template = templates.get(templateIdx);
        else return ResponseEntity.notFound().build();

        try {
            return ResponseEntity.ok(conversationService.getInlineResults(client, conversation, template, creator));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = Result.class, responseContainer = "List",
            notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.POST)
    public ResponseEntity<? extends List<? extends Result>> rerunResults(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") int creator,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: check Authentication / clientId

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

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

    @ApiModel
    class Entity {
        @ApiModelProperty
        private String id;
        @ApiModelProperty
        private URI _uri;

        public String getId() {
            return id;
        }

        public Entity setId(String id) {
            this.id = id;
            return this;
        }

        public URI get_uri() {
            return _uri;
        }

        public Entity set_uri(URI _uri) {
            this._uri = _uri;
            return this;
        }
    }
}
