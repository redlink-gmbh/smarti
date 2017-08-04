package io.redlink.smarti.services;

import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationTarget;

public interface I_ImporterService {
	
	static final String SMARTI_IMPORT_WEBSERVICE = "import";

	/** The postfix to convert the Rocket.Chat query result to a valid JSON. */
	 static final String JSON_RESULT_WRAPPER_END = "]}";
	
	/** The prefix to convert the Rocket.Chat query result to a valid JSON. */
	 static final String JSON_RESULT_WRAPPER_START = "{\"export\": [";
	
	void importComversation(I_ConversationSource source, I_ConversationTarget target) throws Exception;

}
