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

package io.redlink.smarti.webservice.advice;

import io.redlink.smarti.exception.DataException;
import io.redlink.smarti.utils.ResponseEntities;
import io.redlink.smarti.utils.ResponseEntities.ResponseEntityBuilder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.ServletException;
import java.util.Map;

/**
 * Default exception handing
 */
@ControllerAdvice(annotations = RequestMapping.class)
public class GlobalDefaultExceptionHandler {

    @Value("${webservice.errorhandler.writetrace:false}")
    private boolean writeTrace = false;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> defaultErrorHandler(Exception e) throws Exception {
        if (e instanceof ServletException) {
            // ServletException will be handled by Spring
            throw e;
        }

        if (e instanceof AccessDeniedException) {
            // will be handled by Spring
            throw e;
        }

        final HttpStatus status;
        final String statusMessage;
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            status = ObjectUtils.firstNonNull(responseStatus.code(), responseStatus.value());
            statusMessage = responseStatus.reason();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            statusMessage = null;
        }
        final ResponseEntityBuilder reb = ResponseEntities
                .build(status)
                .message(ObjectUtils.firstNonNull(e.getMessage(),statusMessage));

        //DataExceptions can provide structured information about the error that
        //will serialized under the "data" field in the response 
        if(e instanceof DataException<?>){
            Object data = ((DataException<?>)e).getData();
            if(data != null){ //data may be null
                reb.data(data);
            }
        }

        if(writeTrace){
            reb.trace(ExceptionUtils.getStackTrace(e));
        } else {
            log.trace("trace omitted (webservice.errorhandler.writetrace=false)");
        }
        //NOTE: '500 Internal Server Error' responses indicate some internal error that we want to log on WARN level!
        if (status == HttpStatus.INTERNAL_SERVER_ERROR || status == HttpStatus.SERVICE_UNAVAILABLE) {
            if (log.isDebugEnabled()) {
                log.warn("Sending ErrorResponse (status: {}, {}: {}) [Stacktrace FYI]", status, e.getClass().getSimpleName(), e.getMessage(), e);
            } else {
                log.warn("Sending ErrorResponse (status: {}, {}: {})", status, e.getClass().getSimpleName(), e.getMessage());
            }
        } else { //Error indicating that something was wrong with the request so we do not want to log in INFO or higher
            if (log.isTraceEnabled()) {
                log.debug("Sending ErrorResponse (status: {}, {}: {}) [Stacktrace FYI]", status, e.getClass().getSimpleName(), e.getMessage(), e);
            } else {
                log.debug("Sending ErrorResponse (status: {}, {}: {})", status, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return reb.build();
    }

}
