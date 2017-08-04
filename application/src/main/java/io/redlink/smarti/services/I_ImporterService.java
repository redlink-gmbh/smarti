package io.redlink.smarti.services;

import io.redlink.smarti.services.importer.I_ConversationSource;
import io.redlink.smarti.services.importer.I_ConversationTarget;

public interface I_ImporterService {
	
	static final String SMARTI_IMPORT_WEBSERVICE = "import";
	
	void importComversation(I_ConversationSource source, I_ConversationTarget target) throws Exception;

}
