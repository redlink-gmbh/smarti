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

import io.redlink.smarti.auth.SecurityConfigurationProperties;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.webservice.pojo.AuthContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Various helper-methods for checking and asserting access permissions
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
@Service
public class AuthenticationService {

    private final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public static final String ADMIN = "ADMIN";
    public static final String ANONYMOUS = "ANONYMOUS";

    private final SecurityConfigurationProperties securityConfigurationProperties;

    private final UserService userService;

    private final ClientService clientService;

    private final ConversationService conversationService;

    private final AuthTokenService authTokenService;

    @Autowired
    public AuthenticationService(SecurityConfigurationProperties securityConfigurationProperties, UserService userService, ClientService clientService, ConversationService conversationService, AuthTokenService authTokenService) {
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.userService = userService;
        this.clientService = clientService;
        this.conversationService = conversationService;
        this.authTokenService = authTokenService;
    }

    public boolean isAuthenticationEnabled() {
        return securityConfigurationProperties.isEnabled();
    }

    public boolean isAuthenticationDisabled() {
        return !isAuthenticationEnabled();
    }

    public boolean isAuthenticated(AuthContext authentication) {
        return isAuthenticationDisabled() || (Objects.nonNull(authentication) && !hasRole(authentication, ANONYMOUS));
    }

    public boolean hasLogin(Authentication authentication, String login) {
        return isAuthenticationDisabled() || (Objects.nonNull(authentication) && Objects.equals(authentication.getName(), login));
    }

    public boolean hasLogin(AuthContext authContext, String login) {
        return isAuthenticationDisabled() || (Objects.nonNull(authContext) && hasLogin(authContext.getAuthentication(), login));
    }

    public boolean hasLogin(UserDetails authentication, String login) {
        return isAuthenticationDisabled() || (Objects.nonNull(authentication) && Objects.equals(authentication.getUsername(), login));
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
        return isAuthenticationDisabled() || (Objects.nonNull(authContext) && hasAnyRole(authContext.getAuthentication(), roles));
    }

    public boolean hasAnyRole(Authentication authentication, Collection<String> roles) {
        return isAuthenticationDisabled() || (Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> StringUtils.removeStart(r, "ROLE_"))
                        .anyMatch(roles::contains));
    }

    public boolean hasAnyRole(UserDetails authentication, Collection<String> roles) {
        return isAuthenticationDisabled() || (Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> StringUtils.removeStart(r, "ROLE_"))
                        .anyMatch(roles::contains));
    }

    public Set<Client> getClients(Authentication authentication) {
        Set<ObjectId> ids = getClientIds(authentication);
        if(CollectionUtils.isEmpty(ids)){
            return Collections.emptySet();
        } else {
            return StreamSupport.stream(clientService.list(ids).spliterator(),false).collect(Collectors.toSet());
        }
    }
    public Set<ObjectId> getClientIds(Authentication authentication) {
        if (authentication == null) return Collections.emptySet();
        return userService.getClientsForUser(authentication.getName());
    }

    public Set<ObjectId> getClientIds(AuthContext authContext) {
        if (authContext == null) return Collections.emptySet();
        if (StringUtils.isNotBlank(authContext.getAuthToken())) {
            final ObjectId clientId = authTokenService.getClientId(authContext.getAuthToken());
            if (clientId != null) {
                return Collections.singleton(clientId);
            } else {
                return Collections.emptySet();
            }
        }
        return getClientIds(authContext.getAuthentication());
    }
    public Set<Client> getClients(AuthContext authContext) {
        if (authContext == null) return Collections.emptySet();
        if (StringUtils.isNotBlank(authContext.getAuthToken())) {
            final ObjectId clientId = authTokenService.getClientId(authContext.getAuthToken());
            if (clientId != null) {
                Client client = clientService.get(clientId);
                if(client != null){
                    return Collections.singleton(client);
                } else {
                    log.debug("NOT_FOUND client {}", clientId);
                    return Collections.emptySet();
                }
            } else {
                return Collections.emptySet();
            }
        }
        return getClients(authContext.getAuthentication());
    }

    public AuthContext assertRole(AuthContext authContext, String role) {
        if (!hasRole(authContext, role)) {
            log.debug("NOT_IN role '{}': {}", role, authContext);
            throw new AccessDeniedException("Access Denied");
        }

        if (log.isTraceEnabled()) {
            log.trace("ASSERT role {} for {}", role, authContext);
        }
        return authContext;
    }

    public AuthContext assertAnyRole(AuthContext authContext, String... role) {
        if (!hasAnyRole(authContext, role)) {
            log.debug("NOT_IN_ANY role {}: {}", role, authContext);
            throw new AccessDeniedException("Access Denied");
        }

        if (log.isTraceEnabled()) {
            log.trace("ASSERT one role of {} for {}", role, authContext);
        }
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
            if (!getClientIds(authContext).contains(clientId)) {
                log.debug("DENY access to client {} for {}", clientId, authContext);
                throw new NotFoundException(Client.class, clientId);
            }
        }

        final Client client = clientService.get(clientId);
        if (client == null) {
            log.debug("NOT_FOUND client {}", clientId);
            throw new NotFoundException(Client.class, clientId);
        }

        if (log.isTraceEnabled()) {
            log.trace("GRANT access to client {} for {}", clientId, authContext);
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
            log.debug("NOT_FOUND conversation {}", conversationId);
            throw new NotFoundException(Conversation.class, conversationId);
        }

        if (!hasRole(authContext, ADMIN)) {
            if (!getClientIds(authContext).contains(conversation.getOwner())) {
                log.debug("DENY access to conversation {} for {}", conversationId, authContext);
                throw new NotFoundException(Conversation.class, conversationId);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("GRANT access to conversation {} for {}", conversationId, authContext);
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
        if (!isAuthenticated(authContext)) {
            log.debug("UNAUTHENTICATED: {}", authContext);
            throw new AccessDeniedException("Accecss Denied");
        }
        if (log.isTraceEnabled()) {
            log.trace("AUTHENTICATED {}", authContext);
        }
        return authContext;
    }

    public boolean hasAccessToClient(AuthContext authContext, ObjectId clientId) {
        return hasRole(authContext, ADMIN) || getClientIds(authContext).contains(clientId);
    }

    public boolean hasAccessToConversation(AuthContext authContext, ObjectId conversationId) {
        final Conversation conversation = conversationService.getConversation(conversationId);

        return conversation != null
                && (hasRole(authContext, ADMIN) || getClientIds(authContext).contains(conversation.getOwner()));

    }

    public Set<Client> assertClients(AuthContext authContext) {
        final Set<Client> clients = getClients(authContext);
        if (clients.isEmpty()) {
            log.debug("NO_CLIENT for {}", authContext);
            throw new AccessDeniedException("Access Denied");
        }
        if (log.isTraceEnabled()) {
            log.trace("GRANT access to clients {} for {}",
                    clients.stream().map(Client::getId).collect(Collectors.toSet()), authContext);
        }
        return clients;
    }

    public Set<ObjectId> assertClientIds(AuthContext authContext) {
        final Set<ObjectId> clientIds = getClientIds(authContext);
        if (clientIds.isEmpty()) {
            log.debug("NO_CLIENT for {}", authContext);
            throw new AccessDeniedException("Access Denied");
        }
        if (log.isTraceEnabled()) {
            log.trace("GRANT access to clients {} for {}", clientIds, authContext);
        }
        return clientIds;
    }
}
