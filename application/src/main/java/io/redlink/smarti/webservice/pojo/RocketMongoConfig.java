package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"host",
"port",
"dbname",
"room_collection",
"message_collection",
"filter_field",
"filter_value"
})
/**
 * @author Ruediger Kurz (ruediger.kurz@deutschebahn.com)
 * @since 03.08.2017
 */
public class RocketMongoConfig extends A_SourceConfiguration {

	@JsonProperty("dbname")
	private String dbname;

	@JsonProperty("filter_field")
	private String filterField;

	@JsonProperty("filter_value")
	private String filterValue;

	@JsonProperty("host")
	private String host;
	
	@JsonProperty("message_collection")
	private String messageCollection;
	
	@JsonProperty("port")
	private int port;

	@JsonProperty("room_collection")
	private String roomCollection;

	public RocketMongoConfig() {
	}

	public String getDbname() {
		return dbname;
	}

	public String getFilterField() {
		return filterField;
	}

	public String getFilterValue() {
		return filterValue;
	}

	public String getHost() {
		return host;
	}

	public String getMessageCollection() {
		return messageCollection;
	}

	public int getPort() {
		return port;
	}

	public String getRoomCollection() {
		return roomCollection;
	}

	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}

	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setMessageCollection(String messageCollection) {
		this.messageCollection = messageCollection;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setRoomCollection(String roomCollection) {
		this.roomCollection = roomCollection;
	}

}
