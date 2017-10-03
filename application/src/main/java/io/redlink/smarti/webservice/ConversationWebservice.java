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

import io.redlink.smarti.model.*;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.result.Result;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.services.QueryBuilderService;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.QueryUpdate;
import io.redlink.smarti.webservice.pojo.TemplateResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;


/**
 *
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/conversation",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api
public class ConversationWebservice {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @SuppressWarnings("unused")
    private enum Vote {
        up(1),
        down(-1);

        private final int delta;

        Vote(int delta) {
            this.delta = delta;
        }

        public int getDelta() {
            return delta;
        }
    }

    //@Autowired
    //private StoreService storeService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private QueryBuilderService queryBuilderService;
    
    @Autowired
    private ConfigurationService configService;

    @Autowired
    private ClientService clientService;
    
    @ApiOperation(value = "create a conversation")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created", response = Conversation.class)
    })
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createConversation(@RequestBody(required = false) Conversation conversation) {
        conversation = Optional.ofNullable(conversation).orElseGet(Conversation::new);
        // Create a new Conversation -> id must be null
        conversation.setId(null);
        //TODO: set the owner of the conversation based on the current user
        //FIXME for now the owner needs to be parsed with the conversation!
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.update(client, conversation, true, null));
    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getConversation(@PathVariable("id") ObjectId id) {
        //TODO get the client for the authenticated user
        final Conversation conversation = conversationService.getConversation(null, id);
        
        //TODO: check that the client has the right to access the conversation
        
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation);
        }
    }

    @ApiOperation(value = "update a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateConversation(@PathVariable("id") ObjectId id,
                                                @RequestBody Conversation conversation) {
        // make sure the id is the right one
        conversation.setId(id);

        //TODO: check that the authenticated user has rights to update messages of this client
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }
        //TODO: check that the 
        // * authenticated user has rights to update messages of this client
        // * the user is from the client the stored conversation as as owner
        return ResponseEntity.ok(conversationService.update(client, conversation,true, null));
    }

    @ApiOperation(value = "append a message to the conversation", response = Conversation.class)
    @RequestMapping(value = "{id}/message", method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addMessage(@PathVariable("id") ObjectId id,
                                        @RequestBody Message message) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        //TODO: check that the authenticated user has rights to update messages of this client
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }
        return ResponseEntity.ok(conversationService.appendMessage(client, conversation, message));
    }

    @ApiOperation(value = "up-/down-vote a message within a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}/message/{messageId}/{vote}", method = RequestMethod.PUT)
    public ResponseEntity<?> rateMessage(@PathVariable("id") ObjectId id,
                                         @PathVariable("messageId") String messageId,
                                         @PathVariable("vote") Vote vote) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        //TODO: check that the authenticated user has rights to up/down vote the conversation
        return ResponseEntity.ok(conversationService.rateMessage(conversation, messageId, vote.getDelta()));
    }

    @ApiOperation(value = "retrieve the analysis result of the conversation", response = Token.class, responseContainer = "List")
    @RequestMapping(value = "{id}/analysis", method = RequestMethod.GET)
    public ResponseEntity<?> prepare(@PathVariable("id") ObjectId id) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null

        //TODO: check that the authenticated user has rights to access the conversation
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(conversation.getTokens());
        }
    }

    @ApiOperation(value = "retrieve the intents of the conversation", response = TemplateResponse.class)
    @RequestMapping(value = "{id}/template", method = RequestMethod.GET)
    public ResponseEntity<?> query(@PathVariable("id") ObjectId id) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null

        //TODO: check that the authenticated user has rights to access the conversation
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(TemplateResponse.from(conversation));
        }
    }

    @ApiOperation(value = "retrieve the results for a template from a specific creator", response = InlineSearchResult.class)
    @RequestMapping(value = "{id}/template/{template}/{creator}", method = RequestMethod.GET)
    public ResponseEntity<?> getResults(@PathVariable("id") ObjectId id,
                                        @PathVariable("template") int templateIdx,
                                        @PathVariable("creator") String creator,
                                        @RequestParam(required = false) MultiValueMap<String, String> params) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        //TODO: with authentication in place we need to get the Client from the user and NOT the conversation!!
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }

        try {
            final Template template = conversation.getTemplates().get(templateIdx);

            return ResponseEntity.ok(conversationService.getInlineResults(client, conversation, template, creator, params));
        } catch (IOException e) {
            return ResponseEntities.serviceUnavailable(e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "update a query based on new slot-assignments", response = Query.class)
    @RequestMapping(value = "{id}/query/{template}/{creator}", method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getQuery(@PathVariable("id") ObjectId id,
                                      @PathVariable("template") int templateIdx,
                                      @PathVariable("creator") String creator,
                                      @RequestParam(required = false) MultiValueMap<String, String> params,
                                      @RequestBody QueryUpdate queryUpdate) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        //TODO: with authentication in place we need to get the Client from the user and NOT the conversation!!
        Client client = clientService.get(conversation.getOwner());
        if(client == null){
            throw new IllegalStateException("Owner for conversation " + conversation.getId() + " not found!");
        }
        Configuration clientConf = configService.getClientConfiguration(client.getId());
        if(clientConf == null){
            log.info("Client {} of Conversation {} has no longer a configuration assigned ... returning 404 NOT FOUND",
                    conversation.getChannelId(), conversation.getId());
            return ResponseEntity.notFound().build();
        }
        final Template template = conversation.getTemplates().get(templateIdx);
        if (template == null) return ResponseEntity.notFound().build();

        //NOTE: conversationService.getConversation(..) already update the queries if necessary
        //so at this place we only need to retrieve the requested query
        Optional<Query> query = template.getQueries().stream().filter(q -> Objects.equals(creator, q.getCreator())).findFirst();
        return query.isPresent() ? ResponseEntity.ok(query.get()) : ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "complete a conversation and add it to indexing", response = Conversation.class)
    @RequestMapping(value = "{id}/publish", method = RequestMethod.POST)
    public ResponseEntity<?> complete(@PathVariable("id") ObjectId id) {
        //TODO: get the Client for the currently authenticated user 
        final Conversation conversation = conversationService.getConversation(null, id); //TODO: parse the client instead of null

        if (conversation == null) {
            return ResponseEntity.notFound().build();
        } else {
            //TODO: only the owner of the conversation can publish it!
            conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
            return ResponseEntity.ok(conversationService.completeConversation(conversation));
        }
    }

    static class InlineSearchResult extends SearchResult<Result> {

    }
}
