package io.redlink.smarti.services;

import java.io.IOException;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.redlink.smarti.api.StoreService;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.ConversationMeta;
import io.redlink.smarti.model.Message;
import io.redlink.smarti.model.User;
import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationTarget;

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

    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private StoreService storeService;
    
	/**
	 * @see I_ImporterService#importComversation(I_ConversationSource, I_ConversationTarget)
	 */
	@Override
	public void importComversation(I_ConversationSource source, I_ConversationTarget target) throws Exception {

		if (source != null && target != null) {
			long time = new Date().getTime();
			log.info("Start export at    : " + (new Date().getTime() - time) + " ms");
			String export = source.exportConversations();
			log.info("Start parsing at   : " + (new Date().getTime() - time) + " ms");
			JSONObject eportedJSON = (JSONObject) new JSONParser().parse(export);
			log.info("Start import at    : " + (new Date().getTime() - time) + " ms");
			importJSON(source, target, eportedJSON);
			log.info("Finished import at : " + (new Date().getTime() - time) + " ms");
		}
	}

	/**
	 * @param source
	 * @param target
	 * @param export
	 * 
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	private void importJSON(I_ConversationSource source, I_ConversationTarget target, JSONObject export) throws IOException, ClientProtocolException, ParseException {

		Iterator<JSONObject> entryIterator = ((JSONArray) export.get("export")).iterator();
		while (entryIterator.hasNext()) {

			JSONObject expEntry = entryIterator.next();
			if ("r".equals((String) expEntry.get("t"))) {
				// the type of this entry is a room

				String channelId = (String) expEntry.get("_id");
				String channelName = (String) expEntry.get("name");
				
				JSONArray messages = (JSONArray) expEntry.get("messages");
				Iterator<JSONObject> messageIterator = messages.iterator();

				Message m = null;
				Conversation conversation = null;
				
				while (messageIterator.hasNext()) {
					JSONObject message = messageIterator.next();
					Object t = message.get("t");
					if (t == null) {
				        conversation = storeService.getCurrentConversationByChannelId(channelId, () -> {
				            final Conversation newConversation = new Conversation();
				            newConversation.getContext().setContextType(source.getSourceType().toString());
				            newConversation.getContext().setDomain(target.getClientId());
				            newConversation.getContext().setEnvironment("channel", channelName);
				            newConversation.getContext().setEnvironment("channel_id", channelId);
				            newConversation.getContext().setEnvironment("token", target.getToken());
				            return newConversation;
				        });

				        m = new Message();
				        m.setId((String) message.get("_id"));
				        m.setContent((String) message.get("msg"));
						m.setTime(new Date((Long) message.get("_updatedAt")));
				        m.setOrigin(Message.Origin.User);

				        // TODO: Use a UserService to actually *store* the users
				        JSONObject user = (JSONObject) message.get("u");
						final User userO = new User((String) user.get("_id"));
						userO.setDisplayName((String) user.get("username"));
						m.setUser(userO);

				        conversation = conversationService.appendMessage(conversation, m);
				        conversation.getMeta().setStatus(ConversationMeta.Status.Complete);
				        conversationService.completeConversation(conversation);
						log.info(conversation.toString());
					}
				}
			}
		}
	}
}
