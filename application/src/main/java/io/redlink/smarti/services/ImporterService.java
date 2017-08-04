package io.redlink.smarti.services;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationTarget;
import io.redlink.smarti.webservice.ConversationWebservice;
import io.redlink.smarti.webservice.RocketChatEndpoint;
import io.redlink.smarti.webservice.pojo.RocketEvent;

/**
 * This service provides an Smarti importer.<p>
 * 
 * Sources for import data to Smarti are:
 * <li>Directly from Rocket.Chat MongoDB</li>
 * <li>A Rocket.Chat MongoDB Export</li>
 * <li>An E-Mailbox</li>
 * <li>A FAQ website</li>  
 * 
 * @author Ruediger Kurz
 */
@Service
public class ImporterService implements I_ImporterService {
	
	private Logger log = LoggerFactory.getLogger(ImporterService.class);

	/** A simple ISO date formatter. */
	private static final DateFormat ISODateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Autowired
    private ConversationWebservice conversationWebservice;
    
    @Autowired
    private RocketChatEndpoint rcEndpoint;
    
    public ImporterService() {
    		rcEndpoint = new RocketChatEndpoint(null, 0, null);
    }
	
    long time;
    
	/**
	 * @see I_ImporterService#importComversation(I_ConversationSource, I_ConversationTarget)
	 */
	@Override
	public void importComversation(I_ConversationSource source, I_ConversationTarget target) throws Exception {

		
		if (source != null && target != null) {
			time = new Date().getTime();
			log.info("Start export at    : " + (new Date().getTime() - time) + " ms");
			String export = source.exportConversations();
			log.info("Start parsing at   : " + (new Date().getTime() - time) + " ms");
			JSONObject eportedJSON = (JSONObject) new JSONParser().parse(export);
			log.info("Start import at    : " + (new Date().getTime() - time) + " ms");
			importJSON(eportedJSON, target);
			log.info("Finished import at : " + (new Date().getTime() - time) + " ms");
		}
	}

	/**
	 * 
	 * @param obj
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ParseException 
	 */
	private void importJSON(JSONObject export, I_ConversationTarget target) throws IOException, ClientProtocolException, ParseException {

		Iterator<JSONObject> entryIterator = ((JSONArray) export.get("export")).iterator();
		while (entryIterator.hasNext()) {

			JSONObject expEntry = entryIterator.next();
			if ("r".equals((String) expEntry.get("t"))) {
				// the type of this entry is a room

				String channelId = (String) expEntry.get("_id");
				String channelName = (String) expEntry.get("name");
				RocketEvent rocketEvent = null;
				boolean newConversation = false;
				
				JSONArray messages = (JSONArray) expEntry.get("messages");
				Iterator<JSONObject> messageIterator = messages.iterator();

				while (messageIterator.hasNext()) {
					JSONObject message = messageIterator.next();
					Object t = message.get("t");
					if (t == null) {

						rocketEvent = new RocketEvent((String) message.get("_id"));
						rocketEvent.setBot(null);
						rocketEvent.setToken(target.getToken());
						rocketEvent.setChannelId(channelId);
						rocketEvent.setChannelName(channelName);
						rocketEvent.setText((String) message.get("msg"));

						JSONObject user = (JSONObject) message.get("u");
						rocketEvent.setUserId((String) user.get("_id"));
						rocketEvent.setUserId((String) user.get("username"));

						Date timestamp;
						String _updatedAt;
						Object updatedAt = message.get("_updatedAt");
						if (updatedAt instanceof JSONObject) {
							JSONObject jso = (JSONObject) updatedAt;
							String dateAsString = (jso.get("$date")).toString();
							timestamp = new Date(new Long(dateAsString));
							_updatedAt = ISODateFormatter.format(timestamp);
						} else {
							_updatedAt = (String) updatedAt;
							timestamp = ISODateFormatter.parse(_updatedAt);
						}
						rocketEvent.setTimestamp(timestamp);
						rocketEvent.setCallbackUrl((String) message.get("webhook_url"));

						rcEndpoint.onRocketEvent(target.getClientId(), rocketEvent);
						log.info(rocketEvent.toString());
						newConversation = true;
					}
				}
				if (newConversation && rocketEvent != null) {
					ResponseEntity<?> reE = rcEndpoint.getConversation(target.getClientId(), rocketEvent.getChannelId());
					String conversationID = String.valueOf(reE.getBody());
					conversationWebservice.complete(new ObjectId(conversationID));
				}
				newConversation = false;
			}
		}
	}
}
