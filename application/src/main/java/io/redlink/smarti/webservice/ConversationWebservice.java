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
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.*;
import io.swagger.annotations.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 *
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@CrossOrigin
@RestController
@RequestMapping(value = "conversation",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation")
public class ConversationWebservice {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String API_PARAM_CALLBACK = "URI where to POST the result (triggers async processing)";
    private static final String API_ASYNC_NOTE = " If a 'callback' is provided, the request will be processed async.";
    private static final String EDITABLE_CONVERSATION_FIELDS = "meta.status, meta.tags, meta.feedback, context.contextType, context.environmentType, context.domain, context.environment.*";
    private static final String EDITABLE_MESSAGE_FIELDS = "time, origin, content, private, votes, metadata.*";

    private static final String ANALYSIS_STATE = "If enabled the message will be analysed and the analysis results are included in the results";

    private final CallbackService callbackExecutor;
    private final ConversationService conversationService;
    private final AnalysisService analysisService;
    private final ConversationSearchService conversationSearchService;
    private final AuthenticationService authenticationService;

    @Autowired
    public ConversationWebservice(AuthenticationService authenticationService, CallbackService callbackExecutor,
                                  ConversationService conversationService, AnalysisService analysisService,
                                  @Autowired(required = false) ConversationSearchService conversationSearchService) {
        this.callbackExecutor = callbackExecutor;
        this.conversationService = conversationService;
        this.analysisService = analysisService;
        this.conversationSearchService = conversationSearchService;
        this.authenticationService = authenticationService;
    }

    @ApiOperation(value = "list conversations", response = PagedConversationList.class)
    @RequestMapping(method = RequestMethod.GET)
    public Page<ConversationData> listConversations(
            AuthContext authContext,
            @RequestParam(value = "clientId", required = false) List<ObjectId> owners,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        final Set<ObjectId> clientIds = getClientIds(authContext, owners);
        if(CollectionUtils.isNotEmpty(clientIds)){
            return conversationService.listConversations(clientIds, page, pageSize)
                    .map(ConversationData::fromModel);
        } else {
            return new PageImpl<>(Collections.emptyList(), new PageRequest(page, pageSize), 0);
        }

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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @RequestBody(required = false) ConversationData parsedCd,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        final Conversation conversation = parsedCd == null ? new Conversation() : ConversationData.toModel(parsedCd);
        // Create a new Conversation -> id must be null
        conversation.setId(null);
        //TODO: check which fields we allow to be set on create...

        final Client client;
        if (conversation.getOwner() != null) {
            client = authenticationService.assertClient(authContext, conversation.getOwner());
        } else {
            final Set<Client> clients = authenticationService.assertClients(authContext);
            if (clients.size() == 1) {
                client = clients.iterator().next();
            } else {
                return ResponseEntity.unprocessableEntity().build();
            }
        }

        if(client == null){
            throw new IllegalArgumentException("Owner for new conversation not provided!");
        } else {
            conversation.setOwner(client.getId());
        }

        //store the new conversation!
        Conversation created = conversationService.update(client, conversation);

        if(analysis){
            analysisService.analyze(client, created)
                    .whenComplete((a , e) -> {
                        if(a != null){
                            log.debug("callback {} with {}", callback, a);
                            callbackExecutor.execute(callback, CallbackPayload.success(a));
                        } else {
                            log.warn("Analysis of {} failed sending error callback to {} ({} - {})", created.getId(), callback, e, e.getMessage());
                            log.debug("STACKTRACE: ",e);
                            callbackExecutor.execute(callback, CallbackPayload.error(e));
                        }
                    });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildConversationURI(uriBuilder, created.getId())))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, created.getId())))
                .body(ConversationData.fromModel(created));

    }

    @ApiOperation(value = "search for a conversation", response = ConversationSearchResult.class,
            notes = "besides simple text-queries, you can pass in arbitrary solr query parameter.")
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public ResponseEntity<?> searchConversations(
            AuthContext authContext,
            @ApiParam() @RequestParam(value = "client", required = false) List<ObjectId> clients,
            @ApiParam("fulltext search") @RequestParam(value = "text", required = false) String text,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis,
            @ApiParam(hidden = true) @RequestParam MultiValueMap<String, String> queryParams
    ) {

        final Set<ObjectId> clientIds = getClientIds(authContext, clients);

        if (conversationSearchService != null) {
            try {
                return ResponseEntity.ok(conversationSearchService.search(clientIds, queryParams));
            } catch (IOException e) {
                return ResponseEntities.internalServerError(e);
            }
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.GET)
    public ResponseEntity<ConversationData> getConversation(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

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
            AuthContext authContext,
            @PathVariable("conversationId") ObjectId conversationId
    ) {
        authenticationService.assertConversation(authContext, conversationId);

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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(value = "the field to update", required = true, allowableValues = EDITABLE_CONVERSATION_FIELDS) @PathVariable("field") String field,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // Check access to the conversation
        authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if (conversationService.exists(conversationId)) {
            Conversation updated = conversationService.updateConversationField(conversationId, field, data);
            if(analysis){
                analysisService.analyze(client, updated).whenComplete((a , e) -> {
                    if(a != null){
                        log.debug("callback {} with {}", callback, a);
                        callbackExecutor.execute(callback, CallbackPayload.success(a));
                    } else {
                        log.warn("Analysis of {} failed sending error callback to {} ({} - {})", updated.getId(), callback, e, e.getMessage());
                        log.debug("STACKTRACE: ",e);
                        callbackExecutor.execute(callback, CallbackPayload.error(e));
                    }                    
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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(conversation.getMessages());
        }
    }

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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Message message,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        //NOTE: ID generation for sub-documents is not supported by Mongo
        if (StringUtils.isBlank(message.getId())) {
            message.setId(UUID.randomUUID().toString());
        }
        //TODO: should we set the time or is it ok to have messages without time?
        if(message.getTime() == null){
            message.setTime(new Date());
        }
        Conversation c = conversationService.appendMessage(conversation, message);
        final Message created = c.getMessages().stream()
                .filter(m -> Objects.equal(message.getId(), m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Created Message[id: "+message.getId()+"] not present in " + c));

        if(analysis){
            analysisService.analyze(client, c).whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", c.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                }  
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, created.getId())))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                .body(created);
    }

    @ApiOperation(value = "retrieve a message", response = Message.class)
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.GET)
    public ResponseEntity<Message> getMessage(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {

        // Check authentication
        authenticationService.assertConversation(authContext, conversationId);

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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestBody Message message,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // Check authentication
        authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        //make sure the message-id is the addressed one
        message.setId(messageId);
        final Conversation c = conversationService.updateMessage(conversationId, message);
        final Message updated = c.getMessages().stream()
                .filter(m -> Objects.equal(messageId, m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Updated Message[id: "+messageId+"] not present in " + c));
        if(analysis){
            analysisService.analyze(client, c).whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", c.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                } 
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, updated.getId())))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                .body(updated);
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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "true") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        // Check authentication
        authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if(conversationService.deleteMessage(conversationId, messageId)){
            if(analysis){
                Conversation c = conversationService.getConversation(conversationId);
                analysisService.analyze(client, c).whenComplete((a , e) -> {
                    if(a != null){
                        log.debug("callback {} with {}", callback, a);
                        callbackExecutor.execute(callback, CallbackPayload.success(a));
                    } else {
                        log.warn("Analysis of {} failed sending error callback to {} ({} - {})", c.getId(), callback, e, e.getMessage());
                        log.debug("STACKTRACE: ",e);
                        callbackExecutor.execute(callback, CallbackPayload.error(e));
                    } 
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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(value = "the field to update", required = true, allowableValues = EDITABLE_MESSAGE_FIELDS) @PathVariable("field") String field,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(ANALYSIS_STATE) @RequestParam(value = "analysis", defaultValue = "false") boolean analysis,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback,
            @RequestParam(value = "projection", required = false) Projection projection
    ) {
        // Check authentication
        authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        Conversation c = conversationService.updateMessageField(conversationId, messageId, field, data);
        final Message updated = c.getMessages().stream()
                .filter(m -> Objects.equal(messageId, m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Updated Message[id: "+messageId+"] not present in " + c));
        if(analysis){
            analysisService.analyze(client, c).whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", c.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                }                
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildMessageURI(uriBuilder, conversationId, updated.getId())))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                .body(updated);
    }

    @ApiOperation(value = "get the analysis-results of the conversation", response = Analysis.class,
            notes = "retrieve the analysis for this conversation.")
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.GET)
    public ResponseEntity<Analysis> getAnalysis(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) throws InterruptedException, ExecutionException {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        final CompletableFuture<Analysis> analysis = analysisService.analyze(client, conversation);
        if(callback == null){
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .body(analysis.get());
        } else {
            analysis.whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", conversation.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                } 
            });
            return ResponseEntity.accepted()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .build();
        }
    }

    @ApiOperation(value = "re-run analysis based on updated tokens/slot-assignments", response = Analysis.class,
            notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.POST)
    public ResponseEntity<Analysis> rerunAnalysis(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);
        
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
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if(callback != null){
            analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", conversation.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                } 
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

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{conversationId}/analysis/template", method = RequestMethod.GET)
    public ResponseEntity<List<Template>> getTemplates(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if(callback != null){ //async execution with sync ACCEPTED response
            analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a);
                    callbackExecutor.execute(callback, CallbackPayload.success(a));
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", conversation.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                }
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

    @ApiOperation(value = "get a query template", response = Template.class)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}", method = RequestMethod.GET)
    public ResponseEntity<?> getTemplate(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if(callback != null){ //async execution with sync ACCEPTED response
            analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                if(a != null){
                    if(a.getTemplates().size() > templateIdx){
                        log.debug("callback {} with {}", callback, a);
                        callbackExecutor.execute(callback, CallbackPayload.success(a.getTemplates().get(templateIdx)));
                    } else {
                        log.warn("Template[idx:{}] not present in Analysis of {} containing {} templates. Sending '404 not found' callback to {} ", 
                                templateIdx, conversation.getId(), a.getTemplates().size(), callback);
                        callbackExecutor.execute(callback, CallbackPayload.error(new NotFoundException(Template.class, templateIdx)));
                    }
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", conversation.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
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

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = InlineSearchResult.class)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.GET)
    public ResponseEntity<?> getResults(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId
    ) throws IOException {
        //just forward to getResults with analysis == null
        return getResults(authContext, uriBuilder, conversationId, templateIdx, creator, null, clientId, null);
    }

    @ApiOperation(value = "get inline-results for the selected template from the creator", response = InlineSearchResult.class,
            notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.POST)
    public ResponseEntity<?> getResults(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam @RequestParam(value = "client", required = false) ObjectId clientId,
            @ApiParam(API_PARAM_CALLBACK) @RequestParam(value = "callback", required = false) URI callback
    ) throws IOException {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = clientId == null ? null : authenticationService.assertClient(authContext, clientId);

        if(updatedAnalysis != null){
            //TODO: See information at #rerunAnalysis(..)
            return ResponseEntities.status(HttpStatus.NOT_IMPLEMENTED,"parsing an updated analysis not yet supported!");
        }

        if (templateIdx < 0) {
            return ResponseEntity.badRequest().build();
        }
        if(callback != null){
            analysisService.analyze(client,conversation).whenComplete((a , e) -> {
                if(a != null){
                    try {
                        SearchResult<? extends Result> result = execcuteQuery(client, conversation, a, templateIdx, creator);
                        log.debug("callback {} with {}", callback, result);
                        callbackExecutor.execute(callback, CallbackPayload.success(result));
                    } catch(RuntimeException | IOException e1){
                        log.warn("Execution of Query[client: {}, conversation: {}, template: {}, creator: {}] failed sending error callback to {} ({} - {})", 
                                client.getId(), conversation.getId(), templateIdx, creator, callback, e, e.getMessage());
                        log.debug("STACKTRACE: ",e);
                        callbackExecutor.execute(callback, CallbackPayload.error(e1));
                    }
                } else {
                    log.warn("Analysis of {} failed sending error callback to {} ({} - {})", conversation.getId(), callback, e, e.getMessage());
                    log.debug("STACKTRACE: ",e);
                    callbackExecutor.execute(callback, CallbackPayload.error(e));
                }
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildResultURI(uriBuilder, conversationId, templateIdx, creator)))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                .body(execcuteQuery(client, conversation, getAnalysis(client, conversation), templateIdx, creator));
    }

    private Set<ObjectId> getClientIds(AuthContext authContext, Collection<ObjectId> owners) {
        if (CollectionUtils.isNotEmpty(owners)) {
            return owners.stream()
                    .filter(c -> authenticationService.hasAccessToClient(authContext, c))
                    .collect(Collectors.toSet());
        } else {
            return authenticationService.assertClientIds(authContext);
        }
    }

    private SearchResult<? extends Result> execcuteQuery(final Client client, final Conversation conversation, final Analysis analysis, int templateIdx, String creator) throws IOException {
        if (templateIdx < analysis.getTemplates().size()) {
            return analysisService.getInlineResults(client, conversation, analysis, analysis.getTemplates().get(templateIdx), creator);
        } else {
            throw new NotFoundException(Template.class, templateIdx);
        }
    }

    private URI buildConversationURI(UriComponentsBuilder builder, ObjectId conversationId) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId
                ))
                .toUri();
    }

    private URI buildAnalysisURI(UriComponentsBuilder builder, ObjectId conversationId) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", "analysis")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId
                ))
                .toUri();
    }

    private URI buildMessageURI(UriComponentsBuilder builder, ObjectId conversationId, String messageId) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", "message", "{messageId}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "messageId", messageId
                ))
                .toUri();
    }

    private URI buildTokenURI(UriComponentsBuilder builder, ObjectId conversationId, int tokenIdx) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", "analysis", "token", "{tokenIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "tokenIdx", tokenIdx
                ))
                .toUri();
    }

    private URI buildTemplateURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", "analysis", "template", "{templateIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "templateIdx", templateIdx
                ))
                .toUri();
    }

    private URI buildResultURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx, String creator) {
        return builder.cloneBuilder()
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
    @SuppressWarnings("WeakerAccess")
    static class InlineSearchResult extends SearchResult<Result> {}

    @ApiModel
    @SuppressWarnings("WeakerAccess")
    static class ConversationSearchResult extends SearchResult<Conversation> {}
}
