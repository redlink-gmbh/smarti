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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public Iterable<Client> listClients(Authentication authentication) throws IOException {
        if (authenticationService.hasRole(authentication, AuthenticationService.ADMIN)) {
            return clientService.list();
        } else {
            return clientService.list(authenticationService.getClients(authentication));
        }
    }

    @ApiOperation(value = "creates/updates a client", response = Client.class)
    @RequestMapping(method = RequestMethod.POST)
    public Client storeClient(@RequestBody Client client) throws IOException {
        return clientService.save(client);
    }

    @ApiOperation(value = "get a client", response = Client.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public Client getClient(@PathVariable("id") ObjectId id) throws IOException {
        return clientService.get(id);
    }

    @ApiOperation(value = "delete a client")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteClient(@PathVariable("id") ObjectId id) throws IOException {
        if(clientService.exists(id)) {
            clientService.delete(clientService.get(id));
            return ResponseEntity.ok().build();
        } else return ResponseEntities.status(404, "client does not exist");
    }

    @ApiOperation(value = "creates/updates a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.POST)
    public ResponseEntity<?> storeConfig(@PathVariable("id") ObjectId id, @RequestBody(required=true) String jsonData) throws IOException {
        Client client = clientService.get(id);
        if(client == null){
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<?> getClientConfiguration(@PathVariable("id") ObjectId id) throws IOException {
        Client client = clientService.get(id);
        if(client == null){
            return ResponseEntity.notFound().build();
        }
        final Configuration c = configService.getClientConfiguration(client);
        if (c == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ConfigurationWebservice.writeSmartiConfig(c);
        }
    }

    @RequestMapping(value = "{id}/token", method = RequestMethod.GET)
    public ResponseEntity<?> listAuthTokens(@PathVariable("id") ObjectId id) {
        // TODO: check auth!
        return ResponseEntity.ok(authTokenService.getAuthTokens(id));
    }

    @RequestMapping(value = "{id}/token", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthToken(@PathVariable("id") ObjectId id,
                                             @RequestBody(required = false) AuthToken token) {
        String label = "new-token";
        if (token != null) {
            label = token.getLabel();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(authTokenService.createAuthToken(id, label));
    }

    @RequestMapping(value = "{id}/token/{token}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateAuthToken(@PathVariable("id") ObjectId id,
                                             @PathVariable("token") String tokenId,
                                             @RequestBody AuthToken token) {

        final AuthToken updated = authTokenService.updateAuthToken(tokenId, id, token);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    @RequestMapping(value = "{id}/token/{token}", method = RequestMethod.DELETE)
    public ResponseEntity<?> revokeAuthToken(@PathVariable("id") ObjectId id,
                                             @PathVariable("token") String tokenId) {
        if (authTokenService.deleteAuthToken(tokenId, id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping("{id}/user")
    public ResponseEntity<?> listClientUsers(@PathVariable("id") ObjectId id) {
        final Client client = clientService.get(id);
        if (client == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(userService.getUsersForClient(client));
    }

    @RequestMapping(value = "{id}/user", method = RequestMethod.POST)
    public ResponseEntity<?> createClientUser(@PathVariable("id") ObjectId id,
                                              @RequestBody SmartiUser user) {
        final Client client = clientService.get(id);
        if (client == null) return ResponseEntity.notFound().build();

        if (StringUtils.isBlank(user.getUsername())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        user.getClients().clear();

        return ResponseEntity.ok(userService.createUserForClient(user, client));
    }


}
