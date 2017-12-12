/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
@RequestMapping(value = "/debug",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
public class DebugEndpoint {

    private final Logger log = LoggerFactory.getLogger(DebugEndpoint.class);

    private final ObjectMapper om;

    public DebugEndpoint(ObjectMapper objectMapper) {
        om = objectMapper;
    }

    @RequestMapping(value = "{path:.*}", method = RequestMethod.POST)
    public ResponseEntity<?> debugRocketEvent(@PathVariable("path") String path,
                                              @RequestBody Map<String, Object> payload) {
        // Public access
        try {
            log.info("received '{}' event:\n{}", path, om.writeValueAsString(payload));
            return ResponseEntity.accepted().build();
        } catch (JsonProcessingException e) {
            return ResponseEntities.internalServerError(e);
        }
    }



}
