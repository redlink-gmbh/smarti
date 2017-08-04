package io.redlink.smarti.services.importer;

import io.redlink.smarti.webservice.pojo.I_SourceConfiguration;

/**
 * Abstract implementation of a generic data source.<p>
 * 
 * @author Ruediger Kurz
 */
public abstract class A_ConversationSource implements I_ConversationSource {
	
	/** Source type of this data source. */
	private E_SourceType sourceType;
	
	/** Configuration options for this data source. */
	I_SourceConfiguration sourceConfiguration;
	
	/**
	 * Constructor with parameters.<p>
	 * 
	 * @param sourceType the source type
	 * @param sourceConfiguration the configuration options
	 */
	public A_ConversationSource (E_SourceType sourceType, I_SourceConfiguration sourceConfiguration) {

		this.sourceType = sourceType;
		this.sourceConfiguration = sourceConfiguration;
	}

	/**
	 * @see I_ConversationSource#getSourceType()
	 */
	@Override
	public E_SourceType getSourceType() {
		
		return this.sourceType;
	}

	/**
	 * @see I_SourceConfiguration#getSourceConfiguration()
	 */
	@Override
	public I_SourceConfiguration getSourceConfiguration() {

		return this.sourceConfiguration;
	}
}
