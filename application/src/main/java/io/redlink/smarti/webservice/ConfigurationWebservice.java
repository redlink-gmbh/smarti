package io.redlink.smarti.webservice;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerationException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = "/config",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api
public class ConfigurationWebservice {

    private static ObjectMapper objectMapper = new ObjectMapper();
    
    private final ConfigurationService configService;

    public static MapLikeType smartiConfigType;
    
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
    public static ResponseEntity<?> writeSmartiConfig(final Configuration c) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            objectMapper.writerFor(smartiConfigType).writeValue(writer, c.getConfig());
        } catch (JsonGenerationException | JsonMappingException e) {
            return ResponseEntities.internalServerError(e);
        }
        return ResponseEntity.ok(writer.toString());
    }

}
