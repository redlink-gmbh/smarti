/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice;

import io.redlink.smarti.utils.ResponseEntities;
import io.swagger.annotations.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

/**
 *
 */
@CrossOrigin
@RestController
@RequestMapping(value = "conversation",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api("conversation")
public class ConversationWebservice {

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createConversation() {
        return ResponseEntities.notImplemented();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getConversation(@PathVariable("id") String id) {
        return ResponseEntities.notImplemented();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.POST)
    public ResponseEntity<?> addMessage(@PathVariable("id") String id) {
        return ResponseEntities.notImplemented();
    }

    @RequestMapping(value = "{id}/prepare", method = RequestMethod.GET)
    public ResponseEntity<?> prepare(@PathVariable("id") String id) {
        return ResponseEntities.notImplemented();
    }

    @RequestMapping(value = "{id}/query", method = RequestMethod.GET)
    public ResponseEntity<?> query(@PathVariable("id") String id) {
        return ResponseEntities.notImplemented();
    }

}
