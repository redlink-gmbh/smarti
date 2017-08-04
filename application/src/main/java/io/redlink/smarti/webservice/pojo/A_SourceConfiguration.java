package io.redlink.smarti.webservice.pojo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class A_SourceConfiguration implements I_SourceConfiguration {
	
	@Override
	public JSONObject asJSON() {

		try {
			return (JSONObject) new JSONParser().parse(new ObjectMapper().writeValueAsString(this));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String toString() {

		return asJSON().toJSONString();
	}

}
