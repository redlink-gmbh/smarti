package io.redlink.smarti.webservice.pojo;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface I_SourceConfiguration {

	JSONObject asJSON() throws ParseException, JsonProcessingException;
	
	String asString() throws JsonProcessingException;
}
