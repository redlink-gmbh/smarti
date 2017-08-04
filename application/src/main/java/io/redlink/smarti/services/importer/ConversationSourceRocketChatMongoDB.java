package io.redlink.smarti.services.importer;

import java.util.Arrays;
import java.util.Collections;

import org.bson.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

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
			
			ObjectMapper mapper = new ObjectMapper();
			String mappString = mapper.writeValueAsString(Collections.singletonMap("export", ImmutableList.copyOf(iterator)));

			return mappString;
		} finally {
			mongoClient.close();
		}
	}
}
