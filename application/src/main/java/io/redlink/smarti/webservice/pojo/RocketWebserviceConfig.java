package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"rc_endpoint",
"room_collection",
"message_collection",
"filter_field",
"filter_value"
})
/**
 * @author Ruediger Kurz (ruediger.kurz@deutschebahn.com)
 * @since 03.08.2017
 */
public class RocketWebserviceConfig extends A_SourceConfiguration {

	@JsonProperty("rc_endpoint")
	private String rocketChatEndpoint;

	@JsonProperty("room_collection")
	private String roomCollection;
	
	@JsonProperty("message_collection")
	private String messageCollection;
	
	@JsonProperty("filter_field")
	private String filterField;

	@JsonProperty("filter_value")
	private String filterValue;

	public String getRocketChatEndpoint() {
		return rocketChatEndpoint;
	}

	public void setRocketChatEndpoint(String rocketChatEndpoint) {
		this.rocketChatEndpoint = rocketChatEndpoint;
	}

	public String getRoomCollection() {
		return roomCollection;
	}

	public void setRoomCollection(String roomCollection) {
		this.roomCollection = roomCollection;
	}

	public String getMessageCollection() {
		return messageCollection;
	}

	public void setMessageCollection(String messageCollection) {
		this.messageCollection = messageCollection;
	}

	public String getFilterField() {
		return filterField;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}

	public String getFilterValue() {
		return filterValue;
	}

	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}
}
