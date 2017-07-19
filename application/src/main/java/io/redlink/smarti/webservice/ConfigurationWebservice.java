package io.redlink.smarti.webservice;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.config.SmartiConfiguration;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.utils.ResponseEntities;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping(value = "config",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("config")
public class ConfigurationWebservice {

    private final ConfigurationService configService;
    
    public ConfigurationWebservice(ConfigurationService configService) {
        this.configService = configService;
    }

    @ApiOperation(value = "retrieve the configuration for a Client", response = SmartiConfiguration.class)
    @RequestMapping(value = "{client}", method = RequestMethod.GET)
    public ResponseEntity<?> getConfiguration(@PathVariable("client") String client) {
        final Configuration c = configService.getConfiguration(client);
        if(c == null){
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(c.getSmarti());
        }
    }
    @ApiOperation(value = "creates a configuration for a client", response = SmartiConfiguration.class)
    @RequestMapping(value = "{client}", method = RequestMethod.POST)
    public ResponseEntity<?> createConfiguration(@PathVariable("client") String client,
            @RequestBody(required=false) SmartiConfiguration config) {
        if(configService.isConfiguration(client)){
            return ResponseEntities.conflict("A configuration for Client " + client + " already exists!");
        }
        if(config == null){
            config = new SmartiConfiguration();
            //FIXME: init the new config with reasonable defaults
        }
        return ResponseEntity.ok(configService.storeConfiguration(client, config).getSmarti());
    }
    
    @ApiOperation(value = "stores a configuration for a client", response = SmartiConfiguration.class)
    @RequestMapping(value = "{client}", method = RequestMethod.PUT)
    public ResponseEntity<?> storeConfiguration(@PathVariable("client") String client,
            @RequestBody(required=true) SmartiConfiguration config) {
        if(configService.isConfiguration(client)){
            return ResponseEntities.conflict("A configuration for Client " + client + " already exists!");
        }
        if(config == null){
            config = new SmartiConfiguration();
            //FIXME: init the new config with reasonable defaults
        }
        return ResponseEntity.ok(configService.storeConfiguration(client, config).getSmarti());
    }

    
}
