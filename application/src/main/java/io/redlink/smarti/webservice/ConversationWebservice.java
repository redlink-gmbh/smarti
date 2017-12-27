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

import io.redlink.smarti.exception.DataException;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.query.conversation.ConversationSearchService;
import io.redlink.smarti.services.AnalysisService;
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.ClientService;
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
import java.util.concurrent.Future;
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


    private static final String PARAM_CALLBACK = "callback";

    public static final String PARAM_ANALYSIS = "analysis";

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String PARAM_CLIENT_ID = "client";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_PAGE_SIZE = "size";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String PARAM_PROJECTION = "projection";
    private static final String API_ASYNC_NOTE = "<p> If a '<code>callback</code>' is provided, the request will trigger an callback with analysis results"
            + "for the parsed conversation soon as those are available. This callback provides the same information as a subsequent call"
            + "to '<code>GET /conversation/{id}/analysis</code>' <p>.";
    private static final String EDITABLE_CONVERSATION_FIELDS = "<p>Supported fields include <ul>"
            + "<li><code>context.contextType</code>"
            + "<li><code>context.domain</code>"
            + "<li><code>context.environment.*</code> and the shortcut <code>environment.*</code>"
            + "<li><code>meta.status</code> with the shortcut <code>status</code>"
            + "<li><code>meta.*</code> with the shortcut <code>*</code>: however note that <code>meta.*</code> is required for field names that would be mapped to other services of the conversation service."
            + "</ul>";
    private static final String CONVERSATION_FIELD_VALUES = "context.contextType, context.domain, context.environment.*, meta.status, meta.*";
    private static final String EDITABLE_MESSAGE_FIELDS = "<p>Supported fields include <ul>"
            + "<li><code>time</code>: value must be a Date, long time or an ISO date/time"
            + "<li><code>origin</code>: value must be an member of the Orign enumeration"
            + "<li><code>content</code> value must be a String</code>"
            + "<li><code>private</code> value is interpreted as boolean</code>"
            + "<li><code>votes</code> value must be an integer</code>"
            + "<li><code>metadata.*</code> any metadata field"
            + "</ul>";
    private static final String MESSAGE_FIELD_VALUES = "time, origin, content, private, votes, metadata.*";

    private static final String DESCRIPTION_PARAM_CALLBACK = "URI where to POST the Analysis results as soon as they are available (async processing)";
    private static final String DESCRIPTION_PARAM_ANALYSIS = "If enabled the analysis of the conversation is included in the response. This requires the"
            + "request to wait for the analysis to be completed. In case a '<code>callback</code>' is supported and provided the analysis results "
            + "will be POST to this URI instead. In any case requests supporting is parameter will start analysis and cache the results for "
            + "improved response times on subsequent '<code>GET /conversation/{id}/analysis</code>' requests.";
    private static final String DESCRIPTION_PARAM_PROJECTION = "Not yet implemented! Will allow to select different projections over the returned data";
    private static final String DESCRIPTION_PARAM_CLIENT_ID = "If the authentication (by user or auth-token) allows access to multiple clients "
            + "this parameter allows to  specify the clients to be considered for processing the request (intersection of "
            + "assigend to the user and parsed). For processing of some requests only a single client is supported in those"
            + "cases a BAD_REQUST response will be triggered in cases where multiple clients are selected";
    private static final String DESCRIPTION_PARAM_PAGE = "The page number (NOTE that '<code>0</code>' is the first page - 0-indexed)";
    private static final String DESCRIPTION_PARAM_PAGE_SIZE = "The number of elements on a single page";

    private final CallbackService callbackExecutor;
    private final ConversationService conversationService;
    private final AnalysisService analysisService;
    private final ConversationSearchService conversationSearchService;
    private final AuthenticationService authenticationService;
    private final ClientService clientService;


    @Autowired
    public ConversationWebservice(AuthenticationService authenticationService, 
                                  ClientService clientService, ConversationService conversationService, AnalysisService analysisService,
                                  CallbackService callbackExecutor, @Autowired(required = false) ConversationSearchService conversationSearchService) {
        this.callbackExecutor = callbackExecutor;
        this.conversationService = conversationService;
        this.analysisService = analysisService;
        this.clientService = clientService;
        this.conversationSearchService = conversationSearchService;
        this.authenticationService = authenticationService;
    }

    @ApiOperation(value = "list conversations", response = PagedConversationList.class)
    @RequestMapping(method = RequestMethod.GET)
    public Page<ConversationData> listConversations(
            AuthContext authContext,
            @ApiParam(name=PARAM_CLIENT_ID,allowMultiple=true,required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) List<ObjectId> owners,
            @ApiParam(name=PARAM_PAGE,required=false, defaultValue="0", value=DESCRIPTION_PARAM_PAGE) @RequestParam(value = PARAM_PAGE, required = false, defaultValue = "0") int page,
            @ApiParam(name=PARAM_PAGE_SIZE, required=false, defaultValue=DEFAULT_PAGE_SIZE, value=DESCRIPTION_PARAM_PAGE_SIZE) @RequestParam(value = PARAM_PAGE_SIZE, required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
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
            consumes=MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "Create a new Conversation." + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "conversation created", response = ConversationData.class),
    })
    @RequestMapping(method = RequestMethod.POST , consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationData> createConversation(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @RequestBody(required = false) ConversationData parsedCd,
            @ApiParam(name=PARAM_ANALYSIS, required=false, defaultValue="false", value=DESCRIPTION_PARAM_ANALYSIS) @RequestParam(value = PARAM_ANALYSIS, defaultValue = "false") boolean inclAnalysis,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
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

        //trigger analysis
        CompletableFuture<Analysis> analysis = analysisService.analyze(client, created);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
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
        URI conversationUri = buildConversationURI(uriBuilder, created.getId());
        return ResponseEntity.created(conversationUri)
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", conversationUri))
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, created.getId())))
                .body(toConversationData(client, created, analysis, inclAnalysis));

    }

    @ApiOperation(value = "search for a conversation", response = ConversationSearchResult.class,
            notes = "besides simple text-queries, you can pass in arbitrary solr query parameter.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "The results of the search", response = ConversationSearchResult.class),
        @ApiResponse(code = 503, message = "If conversation search is not supported")
    })
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public ResponseEntity<?> searchConversations(
            AuthContext authContext,
            @ApiParam() @RequestParam(value = PARAM_CLIENT_ID, required = false) List<ObjectId> owners,
            @ApiParam(required=false, value="fulltext search") @RequestParam(value = "text", required = false) String text,
            @ApiParam(hidden = true) @RequestParam MultiValueMap<String, String> queryParams
    ) {

        final Set<ObjectId> clientIds = getClientIds(authContext, owners);

        if (conversationSearchService != null) {
            try {
                return ResponseEntity.ok(conversationSearchService.search(clientIds, queryParams));
            } catch (IOException e) {
                return ResponseEntities.internalServerError(e);
            }
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ApiOperation(value = "Retrieve a conversation optionally including analysis results.", response = ConversationData.class)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.GET)
    public ResponseEntity<ConversationData> getConversation(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_ANALYSIS, required=false, defaultValue="false", value=DESCRIPTION_PARAM_ANALYSIS) @RequestParam(value = PARAM_ANALYSIS, defaultValue = "false") boolean inclAnalysis,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        
        final Client client = getResponseClient(authContext, clientId, conversation);

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .body(toConversationData(client, conversation, null, inclAnalysis));
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

    @ApiOperation(value = "update/modify a specific field", response = ConversationData.class,
            notes = "Sets a single property in the Conversation to the value parsed in the payload." 
                    + EDITABLE_CONVERSATION_FIELDS + API_ASYNC_NOTE,
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "field updated (sync)", response = ConversationData.class)
    })
    @RequestMapping(value = "{conversationId}/{field:.*}", method = RequestMethod.PUT, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationData> modifyConversationField(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(value = "the field to update", required = true, allowableValues = CONVERSATION_FIELD_VALUES) @PathVariable("field") String field,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(value = "the new value for the field", required = true) @RequestBody Object data,
            @ApiParam(name=PARAM_ANALYSIS, required=false, defaultValue="false", value=DESCRIPTION_PARAM_ANALYSIS) @RequestParam(value = PARAM_ANALYSIS, defaultValue = "false") boolean inclAnalysis,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
    ) {
        // Check access to the conversation
        Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

        if (conversationService.exists(conversationId)) {
            Conversation updated = conversationService.updateConversationField(conversationId, field, data);
            CompletableFuture<Analysis> analysis = analysisService.analyze(client, updated);
            if(callback != null){
                analysis.whenComplete((a , e) -> {
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
                    .body(toConversationData(client, updated, analysis, inclAnalysis));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @ApiOperation(value = "deletes the specific field", response = ConversationData.class, httpMethod = "DELETE",
            notes = "Deleting a single property in the Conversation." 
                    + EDITABLE_CONVERSATION_FIELDS + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 204, message = "field deleted (sync)", response = Conversation.class)
    })
    @RequestMapping(value = "{conversationId}/{field:.*}", method = RequestMethod.DELETE)
    public ResponseEntity<ConversationData> deleteConversationField(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(value = "the field to delete", required = true, allowableValues = CONVERSATION_FIELD_VALUES) @PathVariable("field") String field,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_ANALYSIS, required=false, defaultValue="false", value=DESCRIPTION_PARAM_ANALYSIS) @RequestParam(value = PARAM_ANALYSIS, defaultValue = "false") boolean inclAnalysis,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
    ) {
        // Check access to the conversation
        Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

        if (conversationService.exists(conversationId)) {
            Conversation updated = conversationService.deleteConversationField(conversationId, field);
            CompletableFuture<Analysis> analysis = analysisService.analyze(client, updated);
            if(callback != null){
                analysis.whenComplete((a , e) -> {
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
                    .body(toConversationData(client, updated, analysis, inclAnalysis));
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
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
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
            consumes=MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "this appends the provided message to the conversation. It is the responsibility of the client to ensure" +
                    "that a messageId passed in is unique. It the client cannot guarantee that, it MUST leave the messageId " +
                    "empty/null." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 201, message = "message created",response=Message.class),
            @ApiResponse(code = 404, message = "conversation not found")
    })
    @RequestMapping(value = "{conversationId}/message", method = RequestMethod.POST, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> appendMessage(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Message message,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        
        final Client client = getResponseClient(authContext, clientId, conversation);

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
                .filter(m -> Objects.equals(message.getId(), m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Created Message[id: "+message.getId()+"] not present in " + c));

        CompletableFuture<Analysis> analysis = analysisService.analyze(client, c);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
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
        URI messageLocation = buildMessageURI(uriBuilder, conversationId, created.getId());
        return ResponseEntity.created(messageLocation)
                .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", messageLocation))
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
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
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
            consumes=MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "fully replace a message." +
                    API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "message updated", response = Message.class),
            @ApiResponse(code = 404, message = "conversation or message not found")
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.PUT, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> updateMessage(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @RequestBody Message message,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback,
            @ApiParam(name=PARAM_PROJECTION, required=false, value=DESCRIPTION_PARAM_PROJECTION) @RequestParam(value = PARAM_PROJECTION, required = false) Projection projection
    ) {
        // Check authentication
        Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

        //make sure the message-id is the addressed one
        message.setId(messageId);
        final Conversation c = conversationService.updateMessage(conversationId, message);
        final Message updated = c.getMessages().stream()
                .filter(m -> Objects.equals(messageId, m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Updated Message[id: "+messageId+"] not present in " + c));
        CompletableFuture<Analysis> analysis = analysisService.analyze(client, c);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
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
            @ApiResponse(code = 404, message = "conversation or message not found")
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteMessage(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        // Check authentication
        Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

        if(conversationService.deleteMessage(conversationId, messageId)){
            Conversation c = conversationService.getConversation(conversationId);
            CompletableFuture<Analysis> analysis = analysisService.analyze(client, c);
            if(callback != null){
                analysis.whenComplete((a , e) -> {
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
            consumes=MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "Sets the property of the Message to the parsed value" 
                    + EDITABLE_MESSAGE_FIELDS + API_ASYNC_NOTE)
    @ApiResponses({
            @ApiResponse(code = 200, message = "field updated Message", response = Message.class),
            @ApiResponse(code = 404, message = "if the conversation or message was not found"),
            @ApiResponse(code = 400, message = "if the value is not valid for the field")
    })
    @RequestMapping(value = "{conversationId}/message/{msgId}/{field}", method = RequestMethod.PUT, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> modifyMessageField(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("msgId") String messageId,
            @ApiParam(value = "the field to update", required = true, allowableValues = MESSAGE_FIELD_VALUES) @PathVariable("field") String field,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(value = "the new value", required = true) @RequestBody Object data,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        // Check authentication
        Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

        Conversation c = conversationService.updateMessageField(conversationId, messageId, field, data);
        final Message updated = c.getMessages().stream()
                .filter(m -> Objects.equals(messageId, m.getId()))
                .findAny().orElseThrow(() -> new IllegalStateException(
                        "Updated Message[id: "+messageId+"] not present in " + c));
        CompletableFuture<Analysis> analysis = analysisService.analyze(client, c);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
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
            notes = "retrieve the analysis for this conversation."
                    + "<p> If a '<code>callback</code>' is provided, the request will trigger an callback with "
                    + "analysis results for the parsed conversation soon as those are available.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "the analysis (sync - with no callback)", response = Analysis.class),
        @ApiResponse(code = 202, message = "accepted with the analysis POST'ed to the callback"),
        @ApiResponse(code = 404, message = "if the conversation is not found")
})
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.GET)
    public ResponseEntity<Analysis> getAnalysis(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) throws InterruptedException, ExecutionException {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);

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
            consumes=MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "<strong>NOT YET IMPLEMENTED!</strong>" + API_ASYNC_NOTE)
    @ApiResponses({
        @ApiResponse(code = 501, message = "not yet implemented")
    })
    @RequestMapping(value = "{conversationId}/analysis", method = RequestMethod.POST, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<Analysis> rerunAnalysis(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);

        final Client client = getResponseClient(authContext, clientId, conversation);
        
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


    @ApiOperation(value = "get the extracted tokes in the conversation", response = Token.class, responseContainer = "List",
            notes = "Returns the tokens of the analysis (sync or async - if callback is present)" + API_ASYNC_NOTE)
    @ApiResponses({
        @ApiResponse(code = 200, message = "The tokens (sync)", response = Token.class, responseContainer = "List"),
        @ApiResponse(code = 202, message = "Accepted (aync): If a callback is provided, the Tokens are POST'ed to"
                + "the callback URI instead", response = Token.class, responseContainer = "List")
    })
    @RequestMapping(value = "{conversationId}/analysis/token", method = RequestMethod.GET)
    public ResponseEntity<List<Token>> getTokens(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = getResponseClient(authContext, clientId, conversation);

        CompletableFuture<Analysis> analysis = analysisService.analyze(client,conversation);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a.getTokens());
                    callbackExecutor.execute(callback, CallbackPayload.success(a.getTokens()));
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
                    .body(waitFor(analysis).getTokens());
        }
    }

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List",
            notes = "Returns the templates of the analysis (sync or async - if callback is present)" + API_ASYNC_NOTE)
    @ApiResponses({
        @ApiResponse(code = 200, message = "The templates (sync)", response = Template.class, responseContainer = "List"),
        @ApiResponse(code = 202, message = "Accepted (aync): The Templates are POST'ed to"
                + "the callback URI instead", response = Template.class, responseContainer = "List")
    })
    @RequestMapping(value = "{conversationId}/analysis/template", method = RequestMethod.GET)
    public ResponseEntity<List<Template>> getTemplates(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = getResponseClient(authContext, clientId, conversation);

        CompletableFuture<Analysis> analysis = analysisService.analyze(client,conversation);
        if(callback != null){ //async execution with sync ACCEPTED response
            analysis.whenComplete((a , e) -> {
                if(a != null){
                    log.debug("callback {} with {}", callback, a.getTemplates());
                    callbackExecutor.execute(callback, CallbackPayload.success(a.getTemplates()));
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
                    .body(waitFor(analysis).getTemplates());
        }

    }

    @ApiOperation(value = "get a query template", response = Template.class,
            notes = "Returns the template of the analysis (sync or async - if callback is present)" + API_ASYNC_NOTE)
    @ApiResponses({
        @ApiResponse(code = 200, message = "The templates (sync)", response = Template.class),
        @ApiResponse(code = 202, message = "Accepted (aync): The Template is POST'ed to the callback URI instead", response = Template.class)
    })
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}", method = RequestMethod.GET)
    public ResponseEntity<?> getTemplate(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(name=PARAM_CALLBACK, required=false, value=DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = getResponseClient(authContext, clientId, conversation);

        CompletableFuture<Analysis> analysis = analysisService.analyze(client,conversation);
        if(callback != null){ //async execution with sync ACCEPTED response
            analysis.whenComplete((a , e) -> {
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
            final List<Template> templates = waitFor(analysis).getTemplates();
            if (templateIdx < templates.size()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildTemplateURI(uriBuilder, conversationId, templateIdx)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                        .body(templates.get(templateIdx));
            } else {
                throw new NotFoundException(Template.class, templateIdx);
            }
        }

    }

    @ApiOperation(value = "get inline-results for the selected template and query creator", 
            response = InlineSearchResult.class)
    @ApiResponses({
        @ApiResponse(code = 200, message = "The search results", response = InlineSearchResult.class),
    })
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.GET)
    public ResponseEntity<?> getResults(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator,
            @ApiParam(name=PARAM_CLIENT_ID, required=false, value=DESCRIPTION_PARAM_CLIENT_ID) @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId
    ) throws IOException {
        //just forward to getResults with analysis == null
        return getResults(authContext, uriBuilder, conversationId, templateIdx, creator, null, clientId, null);
    }

    @ApiOperation(value = "get inline-results for the selected template from the query creator", 
            response = InlineSearchResult.class,
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
            notes = "<strong>Parsing of an Analysis object is NOT IMPLEMENTED</strong>" + API_ASYNC_NOTE)
    @ApiResponses({
        @ApiResponse(code = 200, message = "The search results", response = InlineSearchResult.class),
        @ApiResponse(code = 202, message = "Accepted (aync): The results are POST'ed to the callback URI instead", response = Template.class),
        @ApiResponse(code = 501, message = "If a Analysis is parsed in the request body - <strong>NOT IMPLEMENTED</strong>")
    })
    @RequestMapping(value = "{conversationId}/analysis/template/{templateIdx}/result/{creator}", method = RequestMethod.POST, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getResults(
            AuthContext authContext,
            UriComponentsBuilder uriBuilder,
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("templateIdx") int templateIdx,
            @PathVariable("creator") String creator,
            @RequestBody Analysis updatedAnalysis,
            @ApiParam @RequestParam(value = PARAM_CLIENT_ID, required = false) ObjectId clientId,
            @ApiParam(DESCRIPTION_PARAM_CALLBACK) @RequestParam(value = PARAM_CALLBACK, required = false) URI callback
    ) throws IOException {
        final Conversation conversation = authenticationService.assertConversation(authContext, conversationId);
        final Client client = getResponseClient(authContext, clientId, conversation);

        if(updatedAnalysis != null){
            //TODO: See information at #rerunAnalysis(..)
            return ResponseEntities.status(HttpStatus.NOT_IMPLEMENTED,"parsing an updated analysis not yet supported!");
        }

        if (templateIdx < 0) {
            return ResponseEntity.badRequest().build();
        }
        CompletableFuture<Analysis> analysis = analysisService.analyze(client,conversation);
        if(callback != null){
            analysis.whenComplete((a , e) -> {
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
            return ResponseEntity.accepted()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildResultURI(uriBuilder, conversationId, templateIdx, creator)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"template\"", buildTemplateURI(uriBuilder, conversationId, templateIdx)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"analyse\"", buildAnalysisURI(uriBuilder, conversationId)))
                    .build();
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", buildResultURI(uriBuilder, conversationId, templateIdx, creator)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"template\"", buildTemplateURI(uriBuilder, conversationId, templateIdx)))
                    .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"up\"", buildConversationURI(uriBuilder, conversationId)))
                    .body(execcuteQuery(client, conversation, getAnalysis(client, conversation), templateIdx, creator));
        }
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

    /**
     * This method determines the client based on the parsed autContext an explicitly parsed clientId and
     * optional the conversation as context of the request
     * @param authContext the authContext (required)
     * @param clientId the explicitly parsed client id (optional)
     * @param conversation the conversation as context of the request (optional)
     * @return the client (NOT <code>null</code>)
     */
    private Client getResponseClient(AuthContext authContext, ObjectId clientId, final Conversation conversation) {
        final Client client;
        if(clientId == null){
            Set<ObjectId> clientIds = authenticationService.assertClientIds(authContext);
            if(clientIds.size() == 1){
                clientId = clientIds.iterator().next();
            } else if(conversation != null && clientIds.contains(conversation.getOwner())){
                //try to use the owner of the conversation as preferred. Otherwise
                clientId = conversation.getOwner();
            } else { //multiple possible clients ... BadRequest as clientId is required in such cases
                throw new MultipleClientException(clientIds);
            }
        }
        return authenticationService.assertClient(authContext, clientId);
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
                .pathSegment("conversation", "{conversationId}", PARAM_ANALYSIS)
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
                .pathSegment("conversation", "{conversationId}", PARAM_ANALYSIS, "token", "{tokenIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "tokenIdx", tokenIdx
                ))
                .toUri();
    }

    private URI buildTemplateURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", PARAM_ANALYSIS, "template", "{templateIdx}")
                .buildAndExpand(ImmutableMap.of(
                        "conversationId", conversationId,
                        "templateIdx", templateIdx
                ))
                .toUri();
    }

    private URI buildResultURI(UriComponentsBuilder builder, ObjectId conversationId, int templateIdx, String creator) {
        return builder.cloneBuilder()
                .pathSegment("conversation", "{conversationId}", PARAM_ANALYSIS, "template", "{templateIdx}", "result", "{creator}")
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
    protected ConversationData toConversationData(final Client client, Conversation con, CompletableFuture<Analysis> analysis, boolean inclAnalysis) {
        if(con == null){
            return null;
        }
        ConversationData cd = ConversationData.fromModel(con);
        if(inclAnalysis){
            cd.setAnalysis(analysis == null ? getAnalysis(client, con) : waitFor(analysis));
        }
        return cd;
    }
    /**
     * Helper method that calls {@link AnalysisService#analyze(Client, Conversation)} and
     * cares about Error Handling
     */
    protected Analysis getAnalysis(Client client, Conversation con){
        return waitFor(analysisService.analyze(client, con));
    }
    
    /**
     * Waits for a Future and performs error handling
     */
    protected <T> T waitFor(Future<T> analysis){
        try {
            return analysis.get();
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

    /**
     * Used in cases where no clientId was parsed but the authentication context is valid for multiple
     * clients.
     */
    @ResponseStatus(code=HttpStatus.BAD_REQUEST)
    static class MultipleClientException extends RuntimeException implements DataException<Map<String,Object>> {
        
        private static final long serialVersionUID = -6172514046687880538L;
        private Set<ObjectId> clientIds;

        MultipleClientException(Set<ObjectId> clientIds){
            this.clientIds = clientIds;
        }

        @Override
        public Map<String, Object> getData() {
            Map<String,Object> data = new HashMap<>();
            data.put("client", clientIds);
            data.put("missing", Collections.singletonList("client"));
            return data; 
        }
    }

}
