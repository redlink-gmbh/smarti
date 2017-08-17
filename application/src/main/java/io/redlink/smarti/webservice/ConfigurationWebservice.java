package io.redlink.smarti.webservice;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
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

    @Autowired
    private ObjectMapper objectMapper;
    
    private final ConfigurationService configService;

    private MapLikeType smartiConfigType;
    
    public ConfigurationWebservice(ConfigurationService configService) {
        this.configService = configService;
    }

    @PostConstruct
    protected void initSmartiConfigType(){
        TypeFactory tf = objectMapper.getTypeFactory();
        smartiConfigType = tf.constructMapLikeType(Map.class, 
                tf.constructType(String.class), 
                tf.constructCollectionLikeType(List.class, 
                        ComponentConfiguration.class));
    }

    @ApiOperation(value = "retrieve list of the basic configurations", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getConfigurationComponents() throws IOException {
        return writeSmartiConfig(configService.getDefaultConfiguration());
    }

    @ApiOperation(value = "retrieve the configuration for a Client", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{client}", method = RequestMethod.GET)
    public ResponseEntity<?> getConfiguration(@PathVariable("client") String client) throws IOException {
        final Configuration c = configService.getConfiguration(client);
        if(c == null){
            return ResponseEntity.notFound().build();
        } else {
            return writeSmartiConfig(c);
        }
    }
    /**
     * Internal helper that calls {@link ObjectMapper#writerFor(Class)} by parsing
     * {@link #smartiConfigType} to ensure that {@link JsonTypeInfo} of
     * {@link ComponentConfiguration} is processed and type information are
     * included
     * @param c the configuration to serialize
     * @return the ResponseEntity containing the JSON as String or 
     * {@link ResponseEntities#internalServerError(Exception)} on any JSON serialization
     * problem
     * @throws IOException should never happen as we serialize to a StringWriter
     */
    private ResponseEntity<?> writeSmartiConfig(final Configuration c) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            objectMapper.writerFor(smartiConfigType).writeValue(writer, c.getConfig());
        } catch (JsonGenerationException | JsonMappingException e) {
            return ResponseEntities.internalServerError(e);
        }
        return ResponseEntity.ok(writer.toString());
    }
    
    @ApiOperation(value = "creates a configuration for a client", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{client}", method = RequestMethod.POST)
    public ResponseEntity<?> createConfiguration(@PathVariable("client") String client,
            @RequestBody(required=false) String jsonData) throws IOException {
        if(configService.isConfiguration(client)){
            return ResponseEntities.conflict("A configuration for Client " + client + " already exists!");
        }
        Map<String,List<ComponentConfiguration>> config;
        //FIXME: I am sure their is a Spring was to do this ...
        if(jsonData == null){
            config = configService.getEmptyConfiguration();
        } else {
            try {
                config = objectMapper.readValue(jsonData, smartiConfigType);
            } catch (JsonParseException | JsonMappingException e) {
                return ResponseEntities.badRequest(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return writeSmartiConfig(configService.storeConfiguration(client, config));
    }
    
    @ApiOperation(value = "stores a configuration for a client", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(value = "{client}", method = RequestMethod.PUT)
    public ResponseEntity<?> storeConfiguration(@PathVariable("client") String client,
            @RequestBody(required=true) String jsonData) throws IOException {
        Map<String,List<ComponentConfiguration>> config;
        try {
            config = objectMapper.readValue(jsonData, smartiConfigType);
        } catch (JsonParseException | JsonMappingException e) {
            return ResponseEntities.badRequest(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return writeSmartiConfig(configService.storeConfiguration(client, config));
    }

    
}
