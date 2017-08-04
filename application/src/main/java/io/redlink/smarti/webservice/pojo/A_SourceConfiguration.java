package io.redlink.smarti.webservice.pojo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class A_SourceConfiguration implements I_SourceConfiguration {

	@Override
	public JSONObject asJSON() throws ParseException, JsonProcessingException {

		return (JSONObject) new JSONParser().parse(toString());
	}

	public String asString() throws JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}

}