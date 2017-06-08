/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.redlink.smarti.utils.ResponseEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 */
@CrossOrigin
@RestController
@RequestMapping(value = "debug",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
public class DebugEndpoint {

    private final Logger log = LoggerFactory.getLogger(DebugEndpoint.class);

    private final ObjectMapper om;

    public DebugEndpoint() {
        om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @RequestMapping(value = "{path}", method = RequestMethod.POST)
    public ResponseEntity<?> debugRocketEvent(@PathVariable("path") String path,
                                              @RequestBody Map<String, Object> payload) {
        try {
            log.info("received '{}' event:\n{}", path, om.writeValueAsString(payload));
            return ResponseEntity.accepted().build();
        } catch (JsonProcessingException e) {
            return ResponseEntities.internalServerError(e);
        }
    }



}
