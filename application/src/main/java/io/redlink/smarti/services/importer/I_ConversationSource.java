package io.redlink.smarti.services.importer;

import io.redlink.smarti.webservice.pojo.I_SourceConfiguration;

/**
 * Interface for potential conversational data sources.<p>
 *  
 * @author Ruediger Kurz
 *
 */
public interface I_ConversationSource {

	/** The import source type. */
	static enum E_SourceType {
		RocketChatJSONFile, RocketChatMongoDB, RocketChatWebServie, Mail, FAQs
	}
	
	/**
	 * Returns the type of source.<p>
	 * 
	 * @return the type of source
	 */
	E_SourceType getSourceType();
	
	/**
	 * Returns the source specific configuration options.<p>
	 * 
	 * @return the configuration options
	 */
	I_SourceConfiguration getSourceConfiguration();
	
	/**
	 * Executes the export action and returns the export result as JSON.<p>
	 * 
	 * @return the export as JSON
	 * 
	 * @throws Exception if something goes wrong
	 */
	String exportConversations() throws Exception;
}
