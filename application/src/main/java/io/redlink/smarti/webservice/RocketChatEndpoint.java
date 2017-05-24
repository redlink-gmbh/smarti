/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import io.redlink.smarti.webservice.pojo.RocketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook-Endpoint for rocket.chat
 */
@CrossOrigin
@RestController
@RequestMapping(value = "rocket",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
public class RocketChatEndpoint {

    private Logger log = LoggerFactory.getLogger(RocketChatEndpoint.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> onRocketEvent(@RequestBody RocketEvent payload) {

        log.debug("Payload: {}", payload);

        return ResponseEntity.accepted().build();
    }

}
