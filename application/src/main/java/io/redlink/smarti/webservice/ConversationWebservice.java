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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.query.conversation.ConversationSearchService;
import io.redlink.smarti.services.AnalysisService;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.CallbackPayload;
import io.redlink.smarti.webservice.pojo.ConversationData;
import io.redlink.smarti.webservice.pojo.PagedConversationList;
import io.redlink.smarti.webservice.pojo.Projection;
import io.swagger.annotations.*;
import javassist.bytecode.stackmap.BasicBlock.Catch;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;


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

    private static final String ANALYSIS_STATE = "If enabled the message will be analysed and the analysis results are included in the results";
    
    //private final AsyncExecutionService asyncExecutionService;
    private final CallbackService callbackExecutor;
    private final ConversationService conversationService;
    private final AnalysisService analysisService;
    private final ClientService clientService;
    private final ConversationSearchService conversationSearchService;

    @Autowired
    public ConversationWebservice(CallbackService callbackExecutor, ConversationService conversationService,
                                  AnalysisService analysisService, ClientService clientService,
                                  @Autowired(required = false) ConversationSearchService conversationSearchService) {
        //this.asyncExecutionService = asyncExecutionService;
        this.callbackExecutor = callbackExecutor;
        this.conversationService = conversationService;
        this.analysisService = analysisService;
        this.clientService = clientService;
        this.conversationSearchService = conversationSearchService;
    }

    @ApiOperation(value = "list conversations", response = PagedConversationList.class)
    @RequestMapping(method = RequestMethod.GET)
    public Page<ConversationData> listConversations(
            @RequestParam(value = "clientId", required = false) ObjectId owner,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        //TODO: check authentication, limit clientId to value(s) the user has access to
        
        return conversationService.listConversations(owner, page, pageSize)
                .map(c -> ConversationData.fromModel(c));

    }

    @ApiOperation(value = "create a conversation",
            code = 201, response = Conversation.class,
            notes = "Create a new Conversation." + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "conversation created (sync)", response = Conversation.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Entity.class)
    })
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ConversationData> createConversation(
            UriComponentsBuilder uriBuilder,
            @RequestBody(required = false) ConversationData parsedCd,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis, 
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO: check authentication
        Conversation conversation = parsedCd == null ? new Conversation() : ConversationData.toModel(parsedCd);
        // Create a new Conversation -> id must be null
        conversation.setId(null);
        //TODO: check which fields we allow to be set on create...
        
        //TODO(@jfrank): set the owner of the conversation based on the current auth
        final Client client = clientService
                .get(ObjectUtils.firstNonNull(conversation.getOwner(), conversation.getClientId()));
        if(client == null){
            throw new IllegalStateException("Owner for new conversation not provided!");
        }
        if(analysis){
            analysisService.analyze(client, conversation).whenComplete((a , e) -> {
                callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
            });
        }
        return ResponseEntity.ok(ConversationData.fromModel(conversation));

    }

    @ApiOperation(value = "search for a conversation", response = ConversationSearchResult.class,
            notes = "besides simple text-queries, you can pass in arbitrary solr query parameter.")
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public ResponseEntity<?> searchConversations(
            @ApiParam("fulltext search") @RequestParam(value = "text", required = false) String text,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis, 
            @ApiParam(hidden = true) @RequestParam MultiValueMap<String, String> queryParams
    ) {

        //TODO get the client for the authenticated user
        final Client client = null;

        if (conversationSearchService != null) {
            try {
                return ResponseEntity.ok(conversationSearchService.search(client, queryParams,
                        (c) -> toConversationData(client, c, analysis)));
            } catch (IOException e) {
                return ResponseEntities.internalServerError(e);
            }
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.GET)
    public ResponseEntity<ConversationData> getConversation(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis, 
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO get the client for the authenticated user
        final Client client = null;
        
        final Conversation conversation = conversationService.getConversation(client, conversationId);

        //TODO: check that the client has the right to access the conversation

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(toConversationData(client, conversation, analysis));
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
    public ResponseEntity<ConversationData> modifyConversationField(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(value = "the field to update", required = true, allowableValues = EDITABLE_CONVERSATION_FIELDS) @PathVariable("field") String field,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis, 
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: Check Authentication
        //TODO get the client for the authenticated user
        final Client client = null;

        if (conversationService.exists(conversationId)) {
            Conversation updated = conversationService.updateConversationField(conversationId, field, data);
            if(analysis){
                analysisService.analyze(client, updated).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
                });
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(ConversationData.fromModel(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "list the messages in a conversation", response = Message.class, responseContainer = "List",
            notes = "retrieves all messages in the accessed conversation")
    @RequestMapping(value = "{conversationId}/message", method = RequestMethod.GET)
    public ResponseEntity<List<Message>> listMessages(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        //TODO: check authentication

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(conversation.getMessages());
        }
    }

    //TODO: Should we return a 201 created with the URI or a 200 OK with the message as playload?
    @ApiOperation(value = "create a new message", response=Message.class,
            notes = "this appends the provided message to the conversation. It is the responsibility of the client to ensure" +
                    "that a messageId passed in is unique. It the client cannot guarantee that, it MUST leave the messageId " +
                    "empty/null." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "message created"),
            @ApiResponse(code = 202, message = "accepted for processing (async, aptionally analyed)", 
                response = Conversation.class),
            @ApiResponse(code = 404, message = "conversation not found")
    })
    @RequestMapping(value = "{conversationId}/message", method = RequestMethod.POST)
    public ResponseEntity<?> appendMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Message message,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis, 
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: Check authentication

        final Conversation conversation = conversationService.getConversation(null, conversationId); //TODO: pass the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        //TODO: check that the authenticated user has rights to update messages of this client
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }

        //NOTE: ID generation for sub-documents is not supported by Mongo
        if (StringUtils.isBlank(message.getId())) {
            message.setId(UUID.randomUUID().toString());
        }
        //TODO: should we set the time or is it ok to have messages without time?
        if(message.getTime() == null){
            message.setTime(new Date());
        }
        Conversation c = conversationService.appendMessage(client, conversation, message);
        final Message created = c.getMessages().stream()
                .filter(m -> Objects.equal(message.getId(), m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Created Message[id: "+message.getId()+"] not present in " + c));
        
        if(analysis){
            analysisService.analyze(client, c).whenComplete((a , e) -> {
                callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
            });
        }
        return ResponseEntity.ok()
                //TODO(@jfrank): this will return the Message URI as Location header and a Link header rel="root" to the conversation
                //               Was this meant by the Docu or should the LocationHeader point to the Conversation?
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, created.getId())))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                .body(created);
    }

    @ApiOperation(value = "retrieve a message", response = Message.class)
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.GET)
    public ResponseEntity<Message> getMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        // TODO: Check authentication

        final Message message = conversationService.getMessage(conversationId, messageId);
        if (message == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, messageId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(message);
        }

    }

    @ApiOperation(value = "update/replace a message", response = Message.class,
            notes = "fully replace a message." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "message updated (sync)", response = Message.class),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Conversation.class),
            @ApiResponse(code = 404, message = "conversation or message not found")
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.PUT)
    public ResponseEntity<Message> updateMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestBody Message message,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis, 
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: Check authentication / clientId

        //TODO get the client for the authenticated user
        final Client client = null;

        //make sure the message-id is the addressed one
        message.setId(messageId);
        final Conversation c = conversationService.updateMessage(conversationId, message);
        if(c == null){
            return ResponseEntity.notFound().build();
        } else {
            final Message updated = c.getMessages().stream()
                    .filter(m -> Objects.equal(messageId, m.getId()))
                    .findAny().orElseThrow(() -> new IllegalStateException(
                            "Updated Message[id: "+messageId+"] not present in " + c));
            if(analysis){
                analysisService.analyze(client, c).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
                });
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, updated.getId())))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
            .body(updated);
        }

    }

    @ApiOperation(value = "delete a message", code = 204,
            notes = "delete a message and re-run analysis based on the new conversation." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 204, message = "deleted (no content)"),
            @ApiResponse(code = 202, message = "accepted for processing (async)", response = Conversation.class),
            @ApiResponse(code = 404, message = "conversation or message not found")
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteMessage(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis, 
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: Check Authentication / clientId

        //TODO get the client for the authenticated user
        final Client client = null;

        if(conversationService.deleteMessage(conversationId, messageId)){
            if(analysis){
                Conversation c = conversationService.getConversation(conversationId);
                analysisService.analyze(client, c).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
                });
            }
            return ResponseEntity.noContent()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
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
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // TODO: check Authentication / clientId
        Client client = null;

        // TODO(westei): check that the conversation is re-analyzed/processed
        Conversation c = conversationService.updateMessageField(conversationId, messageId, field, data);
        if(c == null){
            return ResponseEntity.notFound().build();
        } else {
            final Message updated = c.getMessages().stream()
                    .filter(m -> Objects.equal(messageId, m.getId()))
                    .findAny().orElseThrow(() -> new IllegalStateException(
                            "Updated Message[id: "+messageId+"] not present in " + c));
            if(analysis){
                analysisService.analyze(client, c).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
                });
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, updated.getId())))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(updated);
        }
    }

    @ApiOperation(value = "get the analysis-results of the conversation", response = Analysis.class,
            notes = "retrieve the analysis for this conversation.")
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.GET)
    public ResponseEntity<Analysis> getAnalysis(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) throws InterruptedException, ExecutionException {
        // TODO: check Authentication / clientId
        final Client client = null;
        
        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            final CompletableFuture<Analysis> analysis = analysisService.analyze(client, conversation);
            if(callback == null){
                return ResponseEntity.ok()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .body(analysis.get());
            } else {
                analysis.whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a) : CallbackPayload.error(e));
                });
                return ResponseEntity.accepted()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .build();
            }
        }
    }

    @ApiOperation(value = "re-run analysis based on updated tokens/slot-assignments", response = Analysis.class,
    notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.POST)
    public ResponseEntity<Analysis> rerunAnalysis(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: check Authentication / clientId
        
        /* TODO: Not sure how to implement this:
            
             But Analysis is not the correct place to store such Feedback
            for several reasons.
            1. This is about User Feedback! The feedback was provided in a specific state of the 
               conversation. Later messages might invalidate the feedback. We have no way to 
               represent this kind of feedback at the Moment
            2. As this sends the Analysis only we do not know if the conversation has changed
               in the meantime. When we use the parsed Analysis to update Templates and Queries
               we might get inconsistent results as Templates and Queries do have access to both
               the conversation AND the analysis. Therefore they might calculate results with an
               Analysis that does not correspond to the state of the conversation
            3. IMO we need to switch to a context like representation as used by Cerbot. Analysis
               can still be kept, but facts should be moved over to the context. 
               TODO I need to talk with @thomaskurz about this!!
        */
        
        //NOTE: a simple implementation (that ignores above issues) might just take the parsed analysis
        //      and the current state of the Conversation to update Templates and queries.
        
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }


    @ApiOperation(value = "get the extracted tokes in the conversation", response = Token.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/token", method = RequestMethod.GET)
    public ResponseEntity<List<Token>> getTokens(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
   ) {
        // TODO: check Authentication / clientId
        Client client = null;
        // TODO(westei): Check if conversation needs to be re-analyzed/processed
        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            if(callback != null){
                analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a.getTokens()) : CallbackPayload.error(e));
                });
                return ResponseEntity.accepted()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .build();
            } else {
                return ResponseEntity.ok()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .body(getAnalysis(client, conversation).getTokens());
            }
        }

    }

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/template", method = RequestMethod.GET)
    public ResponseEntity<List<Template>> getTemplates(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: check Authentication / clientId
        final Client client = null;

        // TODO(westei): Check if conversation needs to be re-analyzed/processed
        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            if(callback != null){ //async execution with sync ACCEPTED response
                analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                    callbackExecutor.execute(callback, a != null ? CallbackPayload.success(a.getTemplates()) : CallbackPayload.error(e));
                });
                return ResponseEntity.accepted()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .build();
            } else {
                return ResponseEntity.ok()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .body(getAnalysis(client, conversation).getTemplates());
            }
        }

    }

    @ApiOperation(value = "get a query template", response = Template.class)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}", method = RequestMethod.GET)
    public ResponseEntity<?> getTemplate(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // TODO: check Authentication / clientId
        final Client client = null;

        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            if(callback != null){ //async execution with sync ACCEPTED response
                analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                    if(a != null){
                        if(a.getTemplates().size() > templateIdx){
                            callbackExecutor.execute(callback, CallbackPayload.success(a.getTemplates().get(templateIdx)));
                        } else {
                            callbackExecutor.execute(callback, CallbackPayload.error(new NotFoundException(Template.class, templateIdx)));
                        }
                    } else {
                        callbackExecutor.execute(callback, CallbackPayload.error(e));
                    }
                });
                return ResponseEntity.accepted()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildTemplateURI(uriBuilder, conversationId, templateIdx)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .build();
            } else { //sync execution
                final List<Template> templates = getAnalysis(client, conversation).getTemplates();
                if (templateIdx < 0) {
                    return ResponseEntities.badRequest("No template with index "+templateIdx+" present");
                } else if (templateIdx < templates.size()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildTemplateURI(uriBuilder, conversationId, templateIdx)))
                            .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                            .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                            .body(templates.get(templateIdx));
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
        }

    }

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = InlineSearchResult.class)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.GET)
    public ResponseEntity<?> getResults(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator
    ) throws IOException {
        //just forward to getResults with analysis == null
        return getResults(uriBuilder, conversationId, templateIdx, creator, null, null);
    }

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = InlineSearchResult.class,
            notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.POST)
    public ResponseEntity<?> getResults(
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) throws IOException {
        final Client client = null;
        
        if(updatedAnalysis != null){
            //TODO: See information at #rerunAnalysis(..)
            return ResponseEntities.status(HttpStatus.NOT_IMPLEMENTED,"parsing an updated analysis not yet supported!");
        }
        
        final Conversation conversation = conversationService.getConversation(client, conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        if (templateIdx < 0) {
            return ResponseEntity.badRequest().build();
        }
        if(callback != null){
            analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                if(a != null){
                    try {
                        callbackExecutor.execute(callback, CallbackPayload.success(execcuteQuery(client, conversation, a, templateIdx, creator)));
                    } catch(RuntimeException | IOException e1){
                        callbackExecutor.execute(callback, CallbackPayload.error(e1));
                    }
                } else {
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                }
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildResultURI(uriBuilder, conversationId, templateIdx, creator)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .body(execcuteQuery(client, conversation, getAnalysis(client, conversation), templateIdx, creator));
    }
    
    private SearchResult<? extends Result> execcuteQuery(final Client client, final Conversation conversation, final Analysis analysis, int templateIdx, String creator) throws IOException {
        if (templateIdx < analysis.getTemplates().size()) {
            return analysisService.getInlineResults(client, conversation, analysis, analysis.getTemplates().get(templateIdx), creator);
        } else {
            throw new NotFoundException(Template.class, templateIdx);
        }
    }

    private URI buildConversationURI(UriComponentsBuilder builder, ObjectId conversationId) {
        return builder
                .pathSegment("conversation", "{conversationId}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId
                ))
                .toUri();
    }
    
    private URI buildAnalysisURI(UriComponentsBuilder builder, ObjectId conversationId) {
        return builder
                .pathSegment("conversation", "{conversationId}", "analysis")
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

    private URI buildTokenURI(UriComponentsBuilder builder, ObjectId conversationId, int tokenIdx) {
        return builder
                .pathSegment("conversation", "{conversationId}", "analysis", "token", "{tokenIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "tokenIdx", tokenIdx
                ))
                .toUri();
    }
    
    private URI buildTemplateURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx) {
        return builder
                .pathSegment("conversation", "{conversationId}", "analysis", "template", "{templateIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "templateIdx", templateIdx
                ))
                .toUri();
    }
    
    private URI buildResultURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx, String creator) {
        return builder
                .pathSegment("conversation", "{conversationId}", "analysis", "template", "{templateIdx}", "result", "{creator}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "templateIdx", templateIdx,
                        "creator", creator
                ))
                .toUri();
    }

    /**
     * Creates a ConversationData for the parsed parameter
     * @param client the client to build the data for
     * @param con the conversation
     * @param analysis the the analysis should be performed and included in the response
     * @return the ConversationData or <code>null</code> of <code>null</code> was parsed as conversation
     */
    protected ConversationData toConversationData(final Client client, Conversation con, boolean analysis) {
        if(con == null){
            return null;
        }
        ConversationData cd = ConversationData.fromModel(con);
        if(analysis){
            cd.setAnalysis(getAnalysis(client, con));
        }
        return cd;
    }
    /**
     * Helper method that calls {@link AnalysisService#analyze(Client, Conversation)} and
     * cares about Error Handling
     * @param client
     * @param con
     * @return
     */
    protected Analysis getAnalysis(Client client, Conversation con){
        try {
            return analysisService.analyze(client, con).get();
        } catch (ExecutionException e) {
            if(e.getCause() instanceof RuntimeException){
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @ApiModel
    static class Entity {
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

    @ApiModel
    static class InlineSearchResult extends SearchResult<Result> {}

    @ApiModel
    static class ConversationSearchResult extends SearchResult<Conversation> {}
}
