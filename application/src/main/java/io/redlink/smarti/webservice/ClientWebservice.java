package io.redlink.smarti.webservice;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.services.ClientService;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    ClientService clientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigurationService configService;

    @ApiOperation(value = "get a client", response = Client.class)
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<Client> listClients() throws IOException {
        return clientService.list();
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
        if(clientService.exists(id)) {
            String client= clientService.get(id).getName();
            Map<String,List<ComponentConfiguration>> config;
            try {
                config = objectMapper.readValue(jsonData, ConfigurationWebservice.smartiConfigType);
            } catch (JsonParseException | JsonMappingException e) {
                return ResponseEntities.badRequest(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            return ConfigurationWebservice.writeSmartiConfig(configService.storeConfiguration(client, config));
        } else return ResponseEntities.status(404, "client does not exist");
    }

    @ApiOperation(value = "get a client config", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{id}/config", method = RequestMethod.GET)
    public ResponseEntity<?> getClientConfiguration(@PathVariable("id") ObjectId id) throws IOException {
        if(clientService.exists(id)) {
            String client = clientService.get(id).getName();
            final Configuration c = configService.getConfiguration(client);
            if (c == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ConfigurationWebservice.writeSmartiConfig(c);
            }
        } else return ResponseEntities.status(404, "client does not exist");
    }


}
