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
package io.redlink.smarti.services;

import com.google.common.collect.ImmutableSet;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.webservice.pojo.AuthContext;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Service
public class AuthenticationService {

    public static final String ADMIN = "ADMIN";
    public static final String ANONYMOUS = "ANONYMOUS";

    private final UserService userService;
    
    private final ClientService clientService;

    private final ConversationService conversationService;
    
    private final AuthTokenService authTokenService;

    @Autowired
    public AuthenticationService(UserService userService, ClientService clientService, ConversationService conversationService, AuthTokenService authTokenService) {
        this.userService = userService;
        this.clientService = clientService;
        this.conversationService = conversationService;
        this.authTokenService = authTokenService;
    }

    public boolean isAuthenticated(Authentication authentication) {
        return Objects.nonNull(authentication) && !hasRole(authentication, ANONYMOUS);
    }

    public boolean isAuthenticated(AuthContext authentication) {
        return Objects.nonNull(authentication) && !hasRole(authentication, ANONYMOUS);
    }

    public boolean isAuthenticated(UserDetails authentication) {
        return Objects.nonNull(authentication) && !hasRole(authentication, ANONYMOUS);
    }

    public boolean hasUsername(Authentication authentication, String username) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getName(), username);
    }

    public boolean hasUsername(AuthContext authContext, String username) {
        return Objects.nonNull(authContext) && hasUsername(authContext.getAuthentication(), username);
    }

    public boolean hasUsername(UserDetails authentication, String username) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getUsername(), username);
    }

    public boolean hasRole(AuthContext authContext, String role) {
        return hasAnyRole(authContext, role);
    }

    public boolean hasRole(Authentication authentication, String role) {
        return hasAnyRole(authentication, role);
    }

    public boolean hasRole(UserDetails authentication, String role) {
        return hasAnyRole(authentication, role);
    }

    public boolean hasAnyRole(AuthContext authContext, String... roles) {
        return hasAnyRole(authContext, ImmutableSet.copyOf(roles));
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        return hasAnyRole(authentication, ImmutableSet.copyOf(roles));
    }
    public boolean hasAnyRole(UserDetails authentication, String... roles) {
        return hasAnyRole(authentication, ImmutableSet.copyOf(roles));
    }

    public boolean hasAnyRole(AuthContext authContext, Set<String> roles) {
        return Objects.nonNull(authContext) && hasAnyRole(authContext.getAuthentication(), roles);
    }

    public boolean hasAnyRole(Authentication authentication, Set<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roles::contains);
    }

    public boolean hasAnyRole(UserDetails authentication, Set<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roles::contains);
    }

    public Set<ObjectId> getClients(Authentication authentication) {
        return userService.getClientsForUser(authentication.getName());
    }

    public Set<ObjectId> getClients(UserDetails authentication) {
        return userService.getClientsForUser(authentication.getUsername());
    }
    
    public Set<ObjectId> getClients(AuthContext authContext) {
        if (authContext == null) return Collections.emptySet();
        if (StringUtils.isNotBlank(authContext.getAuthToken())) {
            final ObjectId clientId = authTokenService.getClientId(authContext.getAuthToken());
            if (clientId != null) {
                return Collections.singleton(clientId);
            } else {
                return Collections.emptySet();
            }
        }
        return getClients(authContext.getAuthentication());
    }

    public void assertRole(AuthContext authContext, String role) {
        if (!hasRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }

    public void assertRole(Authentication authContext, String role) {
        if (!hasRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }

    public void assertRole(UserDetails authContext, String role) {
        if (!hasRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }

    public void assertAnyRole(AuthContext authContext, String... role) {
        if (!hasAnyRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }

    public void assertAnyRole(Authentication authContext, String... role) {
        if (!hasAnyRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }

    public void assertAnyRole(UserDetails authContext, String... role) {
        if (!hasAnyRole(authContext, role)) throw new AccessDeniedException("Access Denied");
    }


    /**
     * Assert that the {@link AuthContext} allows access to the {@link Client} with the
     * provided id.
     *
     * @param authContext the auth-context
     * @param clientId the client-id
     * @return the {@link Client} for the provided {@code clientId}, if access is granted for the provided {@link AuthContext}
     * @throws AccessDeniedException if the provided {@link AuthContext} does not allow access.
     * @throws NotFoundException if a {@link Client} with the specified {@code clientId} does not exist.
     */
    public Client assertClient(AuthContext authContext, ObjectId clientId) {
        if (!hasRole(authContext, ADMIN)) {
            if (!getClients(authContext).contains(clientId)) {
                throw new NotFoundException(Client.class, clientId);
            }
        }

        final Client client = clientService.get(clientId);
        if (client == null) {
            throw new NotFoundException(Client.class, clientId);
        }
        return client;
    }

    public Conversation assertConversation(AuthContext authContext, ObjectId conversationId) {
        final Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            throw new NotFoundException(Conversation.class, conversationId);
        }

        if (!hasRole(authContext, ADMIN)) {
            if (!getClients(authContext).contains(conversation.getOwner())) {
                throw new NotFoundException(Conversation.class, conversationId);
            }
        }

        return conversation;
    }

    public void assertAuthenticated(AuthContext authContext) {
        if (!isAuthenticated(authContext)) throw new AccessDeniedException("Accecss Denied");
    }

    public boolean hasAccessToClient(AuthContext authContext, ObjectId clientId) {
        return getClients(authContext).contains(clientId);
    }

    public boolean hasAccessToConversation(AuthContext authContext, ObjectId conversationId) {
        final Conversation conversation = conversationService.getConversation(conversationId);

        return conversation != null
                && (hasRole(authContext, ADMIN) || getClients(authContext).contains(conversation.getOwner()));

    }
}
