/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.advice;

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


        if(writeTrace){
            reb.trace(ExceptionUtils.getStackTrace(e));
        } else {
            log.trace("trace omitted (webservice.errorhandler.writetrace=false)");
        }
        log.warn("ErrorResponse (status: {}, {}: {})", status, e.getClass().getSimpleName(), e.getMessage());
        log.debug("STACKTRACE:", e);
        return reb.build();
    }

}
