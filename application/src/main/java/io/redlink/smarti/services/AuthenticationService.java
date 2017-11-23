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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Various helper-methods for checking and asserting access permissions
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
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

    public boolean isAuthenticated(AuthContext authentication) {
        return Objects.nonNull(authentication) && !hasRole(authentication, ANONYMOUS);
    }

    public boolean hasLogin(Authentication authentication, String login) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getName(), login);
    }

    public boolean hasLogin(AuthContext authContext, String login) {
        return Objects.nonNull(authContext) && hasLogin(authContext.getAuthentication(), login);
    }

    public boolean hasLogin(UserDetails authentication, String login) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getUsername(), login);
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
        return hasAnyRole(authContext, Arrays.asList(roles));
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        return hasAnyRole(authentication, Arrays.asList(roles));
    }
    public boolean hasAnyRole(UserDetails authentication, String... roles) {
        return hasAnyRole(authentication, Arrays.asList(roles));
    }

    public boolean hasAnyRole(AuthContext authContext, Collection<String> roles) {
        return Objects.nonNull(authContext) && hasAnyRole(authContext.getAuthentication(), roles);
    }

    public boolean hasAnyRole(Authentication authentication, Collection<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> StringUtils.removeStart(r, "ROLE_"))
                        .anyMatch(roles::contains);
    }

    public boolean hasAnyRole(UserDetails authentication, Collection<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> StringUtils.removeStart(r, "ROLE_"))
                        .anyMatch(roles::contains);
    }

    public Set<ObjectId> getClients(Authentication authentication) {
        return userService.getClientsForUser(authentication.getName());
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

    public AuthContext assertRole(AuthContext authContext, String role) {
        if (!hasRole(authContext, role)) throw new AccessDeniedException("Access Denied");
        return authContext;
    }

    public AuthContext assertAnyRole(AuthContext authContext, String... role) {
        if (!hasAnyRole(authContext, role)) throw new AccessDeniedException("Access Denied");
        return authContext;
    }

    /**
     * Assert that the {@link AuthContext} allows access to the {@link Client} with the
     * provided id.
     *
     * @param authContext the auth-context
     * @param clientId the client-id
     * @return the {@link Client} for the provided {@code clientId}, if access is granted for the provided {@link AuthContext}
     * @throws NotFoundException if the provided {@link AuthContext} does not allow access or the {@link Client} with the specified {@code clientId} does not exist.
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

    /**
     * Assert that the {@link AuthContext} allows access to the {@link Conversation} with the
     * provided id.
     *
     * @param authContext the auth-context
     * @param conversationId the conversation-id
     * @return the {@link Conversation} for the provided {@code conversationId}, if access is granted for the provided {@link AuthContext}
     * @throws NotFoundException if the provided {@link AuthContext} does not allow access or the {@link Conversation} with the specified {@code conversationId} does not exist.
     */
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

    /**
     * Assert that the AuthContext contains a known user.
     *
     * @param authContext the {@link AuthContext} to check
     * @throws AccessDeniedException if un-authorized or anonymous
     */
    public AuthContext assertAuthenticated(AuthContext authContext) {
        if (!isAuthenticated(authContext)) throw new AccessDeniedException("Accecss Denied");
        return authContext;
    }

    public boolean hasAccessToClient(AuthContext authContext, ObjectId clientId) {
        return hasRole(authContext, ADMIN) || getClients(authContext).contains(clientId);
    }

    public boolean hasAccessToConversation(AuthContext authContext, ObjectId conversationId) {
        final Conversation conversation = conversationService.getConversation(conversationId);

        return conversation != null
                && (hasRole(authContext, ADMIN) || getClients(authContext).contains(conversation.getOwner()));

    }

    public Set<Client> assertClients(AuthContext authContext) {
        final Set<Client> clients = assertClientIds(authContext).stream()
                .map(clientService::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (clients.isEmpty()) {
            throw new AccessDeniedException("Access Denied");
        }
        return clients;
    }

    public Set<ObjectId> assertClientIds(AuthContext authContext) {
        final Set<ObjectId> clientIds = getClients(authContext);
        if (clientIds.isEmpty()) {
            throw new AccessDeniedException("Access Denied");
        }
        return clientIds;
    }
}
