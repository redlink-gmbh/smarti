package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RocketWebserviceConfig [rocketChatEndpoint=").append(rocketChatEndpoint)
				.append(", roomCollection=").append(roomCollection).append(", messageCollection=")
				.append(messageCollection).append(", filterField=").append(filterField).append(", filterValue=")
				.append(filterValue).append("]");
		return builder.toString();
	}

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
