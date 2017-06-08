/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 */
public class RocketBot {

    @JsonProperty("i")
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public static class JacksonDeserializer extends JsonDeserializer {
        public RocketBot deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            if(JsonToken.START_OBJECT.equals(parser.getCurrentToken())) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(parser, RocketBot.class);
            } else if (JsonToken.VALUE_FALSE.equals(parser.getCurrentToken())) {
                return null;
            } else
                throw new JsonParseException(parser, "Unexpected token received.");
        }
    }
}
