package io.redlink.smarti.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.redlink.smarti.services.I_ImporterService;
import io.redlink.smarti.services.ImporterService;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatJSONFile;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatMongoDB;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatWebservice;
import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationTarget;
import io.redlink.smarti.webservice.pojo.RocketMongoConfig;
import io.redlink.smarti.webservice.pojo.RocketFileConfig;
import io.redlink.smarti.webservice.pojo.RocketWebserviceConfig;
import io.redlink.smarti.services.importer.ConversationTargetSmarti;
import io.redlink.smarti.services.importer.I_ConversationSource.E_SourceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping(value = I_ImporterService.SMARTI_IMPORT_WEBSERVICE,
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api(I_ImporterService.SMARTI_IMPORT_WEBSERVICE)
public class ImporterServiceEndpoint {

    @Autowired
    private ImporterService importerService;
    
    private Logger log = LoggerFactory.getLogger(ImporterServiceEndpoint.class);
    
    /**
     * This web service should be part of Rocket.Chat not of Smarti.
     * 
     * @param payload
     * 
     * @return the exported chat messages of help request channels
     */
    @ApiOperation(value = "imports the conversation from the given source", produces=MimeTypeUtils.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "{clientId}/{sourceType}", method = RequestMethod.GET,
    		produces=MimeTypeUtils.APPLICATION_JSON_VALUE, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getImport(
    		@PathVariable(value="clientId") String clientId,
    		@PathVariable(value="sourceType") String sourceType,
        @RequestBody String payload) {

    	ObjectMapper mapper = new ObjectMapper();
    	
		try {
	    		E_SourceType type = E_SourceType.valueOf(sourceType);
	    		switch (type) {
	    		case RocketChatJSONFile:
	    			importRocketChatJSONFile(clientId, mapper.readValue(payload, RocketFileConfig.class));
	    			break;
	    		case RocketChatMongoDB:
	    			importRocketChatMongoDB(clientId, mapper.readValue(payload, RocketMongoConfig.class));
	    			break;
	    		case RocketChatWebServie:
	    			importRocketChatWebservice(clientId, mapper.readValue(payload, RocketWebserviceConfig.class));
	    			break;
	    		default:
	    			return ResponseEntity.ok("{status: nosource}");
	    		}
    			return ResponseEntity.ok("{status: sucess}");
    		} catch (Exception e) {
    			return ResponseEntity.status(500).build();
    		}
    }
    
    @ApiOperation(value = "imports the conversation from the given source", produces=MimeTypeUtils.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "{clientId}/rocketChatJSONFile", method = RequestMethod.GET,
    		produces=MimeTypeUtils.APPLICATION_JSON_VALUE, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importRocketChatJSONFile(
    		@PathVariable(value="clientId") String clientId,
        @RequestBody RocketFileConfig fileConfig) {

    		log.warn("called import webservice: '/import/" + clientId + "/rocketChatJSONFile' with config: " + fileConfig.toString());
    		return runImport (new ConversationSourceRocketChatJSONFile(fileConfig), clientId);
    }
    
    @ApiOperation(value = "imports the conversation from the given source", produces=MimeTypeUtils.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "{clientId}/rocketChatMongoDB", method = RequestMethod.GET,
    		produces=MimeTypeUtils.APPLICATION_JSON_VALUE, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importRocketChatMongoDB(
    		@PathVariable(value="clientId") String clientId,
        @RequestBody RocketMongoConfig mongoConfig) {

    		log.warn("called import webservice: '/import/" + clientId + "/rocketChatJSONFile' with config: " + mongoConfig.toString());
    		return runImport (new ConversationSourceRocketChatMongoDB(mongoConfig), clientId);
    }
    
    @ApiOperation(value = "imports the conversation from the given source", produces=MimeTypeUtils.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "{clientId}/rocketChatWebservice", method = RequestMethod.GET,
    		produces=MimeTypeUtils.APPLICATION_JSON_VALUE, consumes=MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importRocketChatWebservice(
    		@PathVariable(value="clientId") String clientId,
        @RequestBody RocketWebserviceConfig webserviceConfig) {
    	
		log.warn("called import webservice: '/import/" + clientId + "/rocketChatJSONFile' with config: " + webserviceConfig.toString());
    		return runImport (new ConversationSourceRocketChatWebservice(webserviceConfig), clientId);
    }
    
    private ResponseEntity<?> runImport (I_ConversationSource source, String clientId) {
    		// TODO: Token not yet implemented, replace key123.
		I_ConversationTarget target = new ConversationTargetSmarti(clientId, "key123");
		try {
			importerService.importComversation(source, target);
			return ResponseEntity.ok("{status: sucess}");
		} catch (Exception e) {
			log.error("Error with message: \"{}\" occured during import: ", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
		
    }
    
}
