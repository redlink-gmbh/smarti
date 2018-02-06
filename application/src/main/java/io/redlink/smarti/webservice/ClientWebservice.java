package io.redlink.smarti.webservice;

import com.google.common.collect.ImmutableMap;
import io.redlink.smarti.model.AuthToken;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.services.*;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.redlink.smarti.webservice.pojo.SmartiUserData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/client")
@Api
public class ClientWebservice {

    private final ClientService clientService;

    private final UserService userService;

    private final ConfigurationService configService;

    private final AuthTokenService authTokenService;

    private final AuthenticationService authenticationService;

    @Autowired
    public ClientWebservice(ClientService clientService, UserService userService, ConfigurationService configService, AuthTokenService authTokenService, AuthenticationService authenticationService) {
        this.clientService = clientService;
        this.userService = userService;
        this.configService = configService;
        this.authTokenService = authTokenService;
        this.authenticationService = authenticationService;
    }

    @ApiOperation(value = "list clients", response = Client.class, responseContainer = "List")
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<Client> listClients(AuthContext authContext) throws IOException {
        if (authenticationService.hasRole(authContext.getAuthentication(), AuthenticationService.ADMIN)) {
            return clientService.list();
        } else {
            return authenticationService.getClients(authContext);
        }
    }

    @ApiOperation(value = "creates/updates a client", response = Client.class)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Client> storeClient(
            AuthContext authContext, 
            @ApiParam(hidden = true) UriComponentsBuilder uriBuilder,
            @RequestBody Client client) throws IOException {
        if (client.getId() == null) { // create, only admin
            authenticationService.assertRole(authContext, AuthenticationService.ADMIN);
        } else {
            authenticationService.assertClient(authContext, client.getId());
        }
        Client created = clientService.save(client);
        return ResponseEntity.created(buildClientURI(uriBuilder, created.getId()))
                .body(created);
    }

    @ApiOperation(value = "get a client", response = Client.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public Client getClient(AuthContext authContext, @PathVariable("id") ObjectId id) throws IOException {
        return authenticationService.assertClient(authContext, id);
    }

    @ApiOperation(value = "delete a client")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteClient(AuthContext authContext,
                                          @PathVariable("id") ObjectId id) throws IOException {
        authenticationService.assertRole(authContext, AuthenticationService.ADMIN);
        if(clientService.exists(id)) {
            clientService.delete(clientService.get(id));
            return ResponseEntity.noContent().build();
        } else return ResponseEntities.status(404, "client does not exist");
    }

    @ApiOperation(value = "creates/updates a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.POST)
    public ResponseEntity<?> storeConfig(AuthContext authContext, @PathVariable("id") ObjectId id,
                                         @RequestBody(required=true) Configuration configuration) throws IOException {
        final Client client = authenticationService.assertClient(authContext, id);

        return ResponseEntity.ok(configService.storeConfiguration(client, configuration.getConfig()));
    }

    @ApiOperation(value = "get a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.GET)
    public ResponseEntity<?> getClientConfiguration(AuthContext authContext, @PathVariable("id") ObjectId id) throws IOException {
        final Client client = authenticationService.assertClient(authContext, id);

        final Configuration c = configService.getClientConfiguration(client);
        if (c == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(c);
        }
    }

    @ApiOperation(value = "retireve auth-tokens for a client", response = AuthToken.class, responseContainer = "Set")
    @RequestMapping(value = "{id}/token", method = RequestMethod.GET)
    public ResponseEntity<List<AuthToken>> listAuthTokens(AuthContext authContext, @PathVariable("id") ObjectId id) {
        final Client client = authenticationService.assertClient(authContext, id);
        return ResponseEntity.ok(authTokenService.getAuthTokens(client.getId()));
    }

    @ApiOperation(value = "create an auth-token", response = AuthToken.class)
    @RequestMapping(value = "{id}/token", method = RequestMethod.POST)
    public ResponseEntity<AuthToken> createAuthToken(AuthContext authContext,
                                             @PathVariable("id") ObjectId id,
                                             @RequestBody(required = false) AuthToken token) {
        final Client client = authenticationService.assertClient(authContext, id);
        String label = "new-token";
        if (token != null) {
            label = token.getLabel();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(authTokenService.createAuthToken(client.getId(), label));
    }

    @ApiOperation(value = "update an auth-token", response = AuthToken.class)
    @RequestMapping(value = "{id}/token/{token}", method = RequestMethod.PUT)
    public ResponseEntity<AuthToken> updateAuthToken(AuthContext authContext,
                                             @PathVariable("id") ObjectId id,
                                             @PathVariable("token") String tokenId,
                                             @RequestBody AuthToken token) {
        final Client client = authenticationService.assertClient(authContext, id);

        final AuthToken updated = authTokenService.updateAuthToken(tokenId, client.getId(), token);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    @ApiOperation("revoke an auth-token")
    @RequestMapping(value = "{id}/token/{token}", method = RequestMethod.DELETE)
    public ResponseEntity<?> revokeAuthToken(AuthContext authContext,
                                             @PathVariable("id") ObjectId id,
                                             @PathVariable("token") String tokenId) {
        final Client client = authenticationService.assertClient(authContext, id);
        if (authTokenService.deleteAuthToken(tokenId, client.getId())) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "list users", notes = "retrieve users assigned to the given client", httpMethod = "GET")
    @RequestMapping("{id}/user")
    public ResponseEntity<List<SmartiUserData>> listClientUsers(AuthContext authContext,
                                                                @PathVariable("id") ObjectId id) {
        final Client client = authenticationService.assertClient(authContext, id);

        return ResponseEntity.ok(userService.getUsersForClient(client).stream()
                .map(SmartiUserData::fromModel)
                .collect(Collectors.toList()));
    }

    @ApiOperation(value = "create user", notes = "create a new user and assign it to the client")
    @RequestMapping(value = "{id}/user", method = RequestMethod.POST)
    public ResponseEntity<SmartiUserData> createClientUser(
            AuthContext authContext,
            @ApiParam(hidden = true) UriComponentsBuilder uriBuilder,
            @PathVariable("id") ObjectId id,
            @RequestBody SmartiUserData user) {
        final Client client = authenticationService.assertClient(authContext, id);

        if (StringUtils.isBlank(user.getLogin())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        user.getClients().clear();
        SmartiUser created = userService.createUserForClient(user.toModel(), client);
        return ResponseEntity.created(buildUserURI(uriBuilder, id, created.getLogin()))
                .body(SmartiUserData.fromModel(created));
    }

    @ApiOperation(value = "assign user", notes = "assign an existing user with the client")
    @RequestMapping(value = "{id}/user/{user}", method = RequestMethod.PUT)
    public ResponseEntity<SmartiUserData> addClientUser(AuthContext authContext,
                                              @PathVariable("id") ObjectId id,
                                              @PathVariable("user") String username) {
        final Client client = authenticationService.assertClient(authContext, id);

        final SmartiUser addedUser = userService.addUserToClient(username, client);
        if (addedUser == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(SmartiUserData.fromModel(addedUser));
        }
    }

    @ApiOperation(value = "remove user", notes = "unassign a user from the client")
    @RequestMapping(value = "{id}/user/{user}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeClientUser(AuthContext authContext,
                                              @PathVariable("id") ObjectId id,
                                              @PathVariable("user") String username) {
        final Client client = authenticationService.assertClient(authContext, id);

        userService.removeUserFromClient(username, client);

        return ResponseEntity.noContent().build();
    }

    private URI buildClientURI(UriComponentsBuilder builder, ObjectId clientId) {
        return builder.cloneBuilder()
                .pathSegment("client", "{clientId}")
                .buildAndExpand(ImmutableMap.of(
                        "clientId", clientId
                ))
                .toUri();
    }
    private URI buildUserURI(UriComponentsBuilder builder, ObjectId clientId, String name) {
        return builder.cloneBuilder()
                .pathSegment("client", "{clientId}","user","{name}")
                .buildAndExpand(ImmutableMap.of(
                        "clientId", clientId,
                        "name", name
                ))
                .toUri();
    }

}
