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

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Token;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.bson.types.ObjectId;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


/**
 *
 */
@CrossOrigin
@RestController
@RequestMapping(value = "conversation",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation")
public class ConversationWebservice {

    @ApiOperation(value = "list conversations", response = Conversation.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public void listConversations() {}

    @ApiOperation(value = "create a conversation", response = Conversation.class)
    @ApiResponses(
            @ApiResponse(code = 201, message = "conversation created")
    )
    @RequestMapping(method = RequestMethod.POST)
    public void createConversation(
            @RequestBody(required = false) Conversation conversation,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
            ) {}

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public void getConversation(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "update/replace a conversation", response = Conversation.class)
    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public void updateConversation(
            @PathVariable("id") ObjectId conversationId,
            @RequestBody(required = false) Conversation conversation,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

    @ApiOperation(value = "delete a conversation", code = 204)
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void deleteConversation(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "update/modifiy a specific field", response = Conversation.class)
    @RequestMapping(value = "{id}/{field:.*}", method = RequestMethod.PUT)
    public void modifyConversationField(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("field") String field,
            @RequestBody Object data,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

    @ApiOperation(value = "list the messages in a conversation", response = Message.class, responseContainer = "List")
    @RequestMapping(value = "{id}/message", method = RequestMethod.GET)
    public void listMessages(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "create a new message", response = Message.class)
    @ApiResponses(
            @ApiResponse(code = 201, message = "message created")
    )
    @RequestMapping(value = "{id}/message", method = RequestMethod.POST)
    public void createMessage(
            @PathVariable("id") ObjectId conversationId,
            @RequestBody(required = false) Message message,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

    @ApiOperation(value = "append a message to the conversation", response = Conversation.class)
    @ApiResponses(
            @ApiResponse(code = 201, message = "message created")
    )
    @RequestMapping(value = "{id}/append-message", method = RequestMethod.POST)
    public void appendMessage(
            @PathVariable("id") ObjectId conversationId,
            @RequestBody(required = false) Message message,
            @RequestParam(value = "callback", required = false)URI callback,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

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
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

    @ApiOperation(value = "update/modifiy a specific filed of the message", response = Message.class)
    @RequestMapping(value = "{id}/message/{msgId}/{field}", method = RequestMethod.POST)
    public void modifyMessageField(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("msgId") ObjectId messageId,
            @PathVariable("field") String field,
            @RequestBody Object data,
            @RequestParam(value = "light", required = false, defaultValue = "false") boolean lightweight
    ) {}

    @ApiOperation(value = "get the extracted tokes in the conversation", response = Token.class, responseContainer = "List")
    @RequestMapping(value = "{id}/token", method = RequestMethod.GET)
    public void getTokens(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "get the (query-)templates in the conversation", response = Template.class, responseContainer = "List")
    @RequestMapping(value = "{id}/template", method = RequestMethod.GET)
    public void getTemplates(
            @PathVariable("id") ObjectId conversationId
    ) {}

    @ApiOperation(value = "get a query template", response = Template.class)
    @RequestMapping(value = "{id}/template/{tmplIdx}", method = RequestMethod.GET)
    public void getTemplate(
            @PathVariable("id") ObjectId conversationId,
            @PathVariable("tmplIdx") int templateIdx
    ) {}

}
