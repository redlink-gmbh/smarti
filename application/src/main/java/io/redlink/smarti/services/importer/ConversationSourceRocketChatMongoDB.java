package io.redlink.smarti.services.importer;

import java.util.Arrays;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import io.redlink.smarti.services.I_ImporterService;
import io.redlink.smarti.webservice.pojo.RocketMongoConfig;

public class ConversationSourceRocketChatMongoDB extends A_ConversationSource {

	RocketMongoConfig mongoConfig;

	public ConversationSourceRocketChatMongoDB(RocketMongoConfig config) {

		super(E_SourceType.RocketChatMongoDB, config);
		mongoConfig = config;
	}

	@Override
	public String exportConversations() throws Exception {
		
		MongoClient mongoClient = new MongoClient(mongoConfig.getHost(), mongoConfig.getPort());
		
		try {
			MongoDatabase db = mongoClient.getDatabase(mongoConfig.getDbname());
			MongoCollection<Document> coll = db.getCollection(mongoConfig.getRoomCollection());
			// get all rooms of type 'request' and expertise 'x' joint with messages 
			MongoCursor<Document> iterator = coll.aggregate(Arrays.asList(
					Aggregates.match(Filters.eq("t", "r")),
					Aggregates.match(Filters.regex(mongoConfig.getFilterField(), mongoConfig.getFilterValue())),
					Aggregates.lookup(mongoConfig.getMessageCollection(), "_id", "rid", "messages"))).iterator();
			
			StringBuffer input = new StringBuffer();
			input.append(I_ImporterService.JSON_RESULT_WRAPPER_START);
			
			while (iterator.hasNext()) {
				Document doc = iterator.next();
				String jsonDoc = doc.toJson();
				input.append(jsonDoc);
				System.out.println(jsonDoc);
				if (iterator.hasNext()) {
					input.append(", ");
				}
			}
			input.append(I_ImporterService.JSON_RESULT_WRAPPER_END);
			
			return input.toString();
		} finally {
			mongoClient.close();
		}
	}
}
