package io.redlink.smarti.webservice;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.redlink.smarti.model.AuthToken;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.services.*;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
@CrossOrigin
@RestController
@RequestMapping(value = "client")
@Api("client")
public class ClientWebservice {

    private final ClientService clientService;

    private final UserService userService;

    private final ObjectMapper objectMapper;

    private final ConfigurationService configService;

    private final AuthTokenService authTokenService;

    private final AuthenticationService authenticationService;

    @Autowired
    public ClientWebservice(ClientService clientService, UserService userService, ObjectMapper objectMapper, ConfigurationService configService, AuthTokenService authTokenService, AuthenticationService authenticationService) {
        this.clientService = clientService;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
            return clientService.list(authenticationService.getClients(authContext));
        }
    }

    @ApiOperation(value = "creates/updates a client", response = Client.class)
    @RequestMapping(method = RequestMethod.POST)
    public Client storeClient(AuthContext authContext, @RequestBody Client client) throws IOException {
        if (client.getId() == null) { // create, only admin
            authenticationService.assertRole(authContext, AuthenticationService.ADMIN);
        } else {
            authenticationService.assertClient(authContext, client.getId());
        }
        return clientService.save(client);
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
            return ResponseEntity.ok().build();
        } else return ResponseEntities.status(404, "client does not exist");
    }

    @ApiOperation(value = "creates/updates a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.POST)
    public ResponseEntity<?> storeConfig(AuthContext authContext, @PathVariable("id") ObjectId id, @RequestBody(required=true) String jsonData) throws IOException {
        final Client client = authenticationService.assertClient(authContext, id);

        Map<String,List<ComponentConfiguration>> config;
        try {
            config = objectMapper.readValue(jsonData, ConfigurationWebservice.smartiConfigType);
        } catch (JsonParseException | JsonMappingException e) {
            return ResponseEntities.badRequest(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return ConfigurationWebservice.writeSmartiConfig(configService.storeConfiguration(client, config));
    }

    @ApiOperation(value = "get a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.GET)
    public ResponseEntity<?> getClientConfiguration(AuthContext authContext, @PathVariable("id") ObjectId id) throws IOException {
        final Client client = authenticationService.assertClient(authContext, id);

        final Configuration c = configService.getClientConfiguration(client);
        if (c == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ConfigurationWebservice.writeSmartiConfig(c);
        }
    }

    @RequestMapping(value = "{id}/token", method = RequestMethod.GET)
    public ResponseEntity<?> listAuthTokens(AuthContext authContext, @PathVariable("id") ObjectId id) {
        final Client client = authenticationService.assertClient(authContext, id);
        return ResponseEntity.ok(authTokenService.getAuthTokens(client.getId()));
    }

    @RequestMapping(value = "{id}/token", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthToken(AuthContext authContext,
                                             @PathVariable("id") ObjectId id,
                                             @RequestBody(required = false) AuthToken token) {
        final Client client = authenticationService.assertClient(authContext, id);
        String label = "new-token";
        if (token != null) {
            label = token.getLabel();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(authTokenService.createAuthToken(client.getId(), label));
    }

    @RequestMapping(value = "{id}/token/{token}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateAuthToken(AuthContext authContext,
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

    @RequestMapping("{id}/user")
    public ResponseEntity<?> listClientUsers(AuthContext authContext,
                                             @PathVariable("id") ObjectId id) {
        final Client client = authenticationService.assertClient(authContext, id);

        return ResponseEntity.ok(userService.getUsersForClient(client));
    }

    @RequestMapping(value = "{id}/user", method = RequestMethod.POST)
    public ResponseEntity<?> createClientUser(AuthContext authContext,
                                              @PathVariable("id") ObjectId id,
                                              @RequestBody SmartiUser user) {
        final Client client = authenticationService.assertClient(authContext, id);

        if (StringUtils.isBlank(user.getUsername())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        user.getClients().clear();

        return ResponseEntity.ok(userService.createUserForClient(user, client));
    }

    @RequestMapping(value = "{id}/user/{user}", method = RequestMethod.PUT)
    public ResponseEntity<?> addClientUser(AuthContext authContext,
                                              @PathVariable("id") ObjectId id,
                                              @PathVariable("user") String username) {
        final Client client = authenticationService.assertClient(authContext, id);

        final SmartiUser addedUser = userService.addUserToClient(username, client);
        if (addedUser == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(addedUser);
        }
    }

    @RequestMapping(value = "{id}/user/{user}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeClientUser(AuthContext authContext,
                                              @PathVariable("id") ObjectId id,
                                              @PathVariable("user") String username) {
        final Client client = authenticationService.assertClient(authContext, id);

        userService.removeUserFromClient(username, client);

        return ResponseEntity.noContent().build();
    }


}
