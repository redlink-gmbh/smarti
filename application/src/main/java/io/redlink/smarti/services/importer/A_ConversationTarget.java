package io.redlink.smarti.services.importer;

public abstract class A_ConversationTarget implements I_ConversationTarget {

	private String clientId;
	private String token;
	
	public A_ConversationTarget (String clientId, String token)  {
		this.clientId = clientId;
		this.token = token;
	}
	
	@Override
	public String getClientId() {
		return clientId;
	}
	
	@Override
	public String getToken() {
		return token;
	}
}
