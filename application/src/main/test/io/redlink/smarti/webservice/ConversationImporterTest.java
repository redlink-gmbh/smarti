package io.redlink.smarti.webservice;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.redlink.smarti.services.ImporterService;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatJSONFile;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatMongoDB;
import io.redlink.smarti.services.importer.ConversationSourceRocketChatWebservice;
import io.redlink.smarti.services.importer.ConversationTargetSmarti;
import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationSource.E_SourceType;
import io.redlink.smarti.services.importer.I_ConversationTarget;
import io.redlink.smarti.webservice.pojo.RocketFileConfig;
import io.redlink.smarti.webservice.pojo.RocketMongoConfig;
import io.redlink.smarti.webservice.pojo.RocketWebserviceConfig;

/**
 * 
 * @author Ruediger Kurz (ruediger.kurz@deutschebahn.com)
 * @since 03.08.2017
 */
public class ConversationImporterTest {

	/** The database name of Rocket.Chat. */
    private static final String ROCKETCHAT_MONGO_DB_NAME = "rocketchat";
	
	/** The name of the Rocket.Chat collection that holds the chat messages. */
	private static final String ROCKETCHAT_MESSAGE_COLLECTION = "rocketchat_message";
	
	/** The name of the Rocket.Chat collection that holds the rooms.  */
	private static final String ROCKETCHAT_ROOM_COLLECTION = "rocketchat_room";
    
	/** The host name of the Rocket.Chat MongoDB server. */
	private static final String ROCKETCHAT_MONGO_HOST = "localhost";
	
	/** The port number of the Rocket.Chat MongoDB. */
	private static final int ROCKETCHAT_MONGO_PORT = 27017;
	
	/** The name of a room field/property that is used to filter the rooms that should be exported from Rocket.Chat. */
	private static final String ROCKETCHAT_FILTER_ROOM_FIELD_NAME = "expertise";
	
	/** The value of the room field that must match; a regex can be used. */
	private static final String ROCKETCHAT_FILTER_ROOM_FIELD_VALUE = "assistify";

	/** The absolute path to the Rocket.Chat JSON export. */
	private static final String FILE_IMPORT_SOURCE = "file:///Users/rudiger/projects/smarti/application/src/test/resources/export-sapsus-faqs.json";
	
	/** The id of the client that is used to identify the knowledge domain in Smarti. */
	private static final String SMARTI_CLIENT_ID = "test.assistify.de";
	
    private LocalTestServer server = new LocalTestServer(null, null);

    @Before
    public void setUp() throws Exception {
        server.start();
        server.register("*", (httpRequest, httpResponse, httpContext) ->
                httpResponse.setEntity(new StringEntity("foobar")));
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }
    
	@Test
	public void testJSONFile() throws ClientProtocolException, IOException, Exception {
		runImport(E_SourceType.RocketChatJSONFile);
	}
	
	private void runImport(E_SourceType type) throws ClientProtocolException, IOException, Exception {
		
		I_ConversationTarget target = new ConversationTargetSmarti(SMARTI_CLIENT_ID, "key123");
		I_ConversationSource source = null;
		
		switch (type) {
		
			case RocketChatJSONFile:
				RocketFileConfig fileConfig = new RocketFileConfig();
				fileConfig.setJsonFileURL(FILE_IMPORT_SOURCE);
				source = new ConversationSourceRocketChatJSONFile (fileConfig);
				break;
				
			case RocketChatMongoDB:
				RocketMongoConfig mongoConfig = new RocketMongoConfig();
				mongoConfig.setHost(ROCKETCHAT_MONGO_HOST);
				mongoConfig.setPort(ROCKETCHAT_MONGO_PORT);
				mongoConfig.setDbname(ROCKETCHAT_MONGO_DB_NAME);
				mongoConfig.setRoomCollection(ROCKETCHAT_ROOM_COLLECTION);
				mongoConfig.setMessageCollection(ROCKETCHAT_MESSAGE_COLLECTION);
				mongoConfig.setFilterField(ROCKETCHAT_FILTER_ROOM_FIELD_NAME);
				mongoConfig.setFilterValue(ROCKETCHAT_FILTER_ROOM_FIELD_VALUE);
				source = new ConversationSourceRocketChatMongoDB(mongoConfig);
				break;
				
			case RocketChatWebServie:
				RocketWebserviceConfig webserviceConfig = new RocketWebserviceConfig();
				webserviceConfig.setRocketChatEndpoint("magic");
				webserviceConfig.setRoomCollection(ROCKETCHAT_ROOM_COLLECTION);
				webserviceConfig.setMessageCollection(ROCKETCHAT_MESSAGE_COLLECTION);
				webserviceConfig.setFilterField(ROCKETCHAT_FILTER_ROOM_FIELD_NAME);
				webserviceConfig.setFilterValue(ROCKETCHAT_FILTER_ROOM_FIELD_VALUE);
				source = new ConversationSourceRocketChatWebservice(webserviceConfig);
				break;
				
			case FAQs:
				break;
				
			case Mail:
				break;
				
			default:
				break;
		}
		
		new ImporterService().importComversation(source, target);
	}
}
