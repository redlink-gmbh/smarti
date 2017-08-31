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
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.utils.WebserviceUtils;
import io.redlink.smarti.webservice.pojo.PagedConversationList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping(value = "admin/conversation",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation-admin")
public class ConversationAdminWebservice {

    private final ConversationService conversationService;

    public ConversationAdminWebservice(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @ApiOperation(value = "list conversations", response = PagedConversationList.class)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> listConversations(
            @RequestParam(value = "clientId", required = false) String clientId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        WebserviceUtils.checkParameter(page >= 0, "page must not be negative");
        WebserviceUtils.checkParameter(pageSize > 0, "minimal pageSize is 1");

        return ResponseEntity.ok(conversationService.listConversations(clientId, page, pageSize));
    }

    @ApiOperation(value = "retrieve a conversation", response = Conversation.class)
    @RequestMapping(value = "{conversationId}", method = RequestMethod.GET)
    public ResponseEntity<?> getConversation(
            @PathVariable("conversationId") ObjectId conversationId) {
        final Conversation conversation = conversationService.getConversation(conversationId);
        if (Objects.nonNull(conversation)) {
            return ResponseEntity.ok(conversation);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "delete a message", response = Conversation.class)
    @RequestMapping(value = "{conversationId}/message/{messageId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteMessage(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("messageId") String messageId) {
        return ResponseEntities.notImplemented();
    }

    @ApiOperation(value = "edit/update a message", response = Conversation.class)
    @RequestMapping(value = "{conversationId}/message/{messageId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateMessage(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("messageId") String messageId,
            @RequestBody Message updatedMessage) {
        // Make sure the messageId does not change
        updatedMessage.setId(messageId);
        return ResponseEntities.notImplemented();
    }

    @ApiOperation(value = "set/update the status of the conversation", response = Conversation.class)
    @RequestMapping(value = "{conversationId}/status/{newStatus}", method = RequestMethod.PUT)
    public ResponseEntity<?> setConversationStatus(
            @PathVariable("conversationId") ObjectId conversationId,
            @PathVariable("newStatus") @ApiParam(allowableValues = "New,Ongoing,Complete", required = true) ConversationMeta.Status newStatus) {
        return ResponseEntities.notImplemented();
    }

}
