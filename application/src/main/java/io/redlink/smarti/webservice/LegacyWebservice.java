/*
 * Copyright 2018 Redlink GmbH
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

import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.ConversationService;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Api
@CrossOrigin
@RestController
@RequestMapping(value = "/legacy", produces = MediaType.APPLICATION_JSON_VALUE)
public class LegacyWebservice {

    private final AuthenticationService authenticationService;

    private final ConversationService conversationService;

    public LegacyWebservice(AuthenticationService authenticationService, ConversationService conversationService) {
        this.authenticationService = authenticationService;
        this.conversationService = conversationService;
    }

    @ApiOperation(value = "find conversation by channel_id", response = Conversation.class)
    @RequestMapping(value = "rocket.chat", method = RequestMethod.GET)
    public ResponseEntity<Conversation> findConversation(AuthContext authContext,
                                                         @RequestParam(value = "client_id", required = false) ObjectId clientId,
                                                         @RequestParam(value = "channel_id") String channel_id) {
        final String context_type = "rocket.chat";
        final Client owner;
        if (clientId != null) {
            owner = authenticationService.assertClient(authContext, clientId);
        } else {
            final Set<Client> clients = authenticationService.assertClients(authContext);
            if (clients.size() != 1) {
                return ResponseEntity.badRequest().build();
            }
            owner = clients.iterator().next();
        }

        final Conversation legacyConversation = conversationService.findLegacyConversation(owner, context_type, channel_id);
        if (legacyConversation == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(legacyConversation);
    }

}
