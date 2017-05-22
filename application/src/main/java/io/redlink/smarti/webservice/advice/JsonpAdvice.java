/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.webservice.advice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

/**
 * Enable JsonP callbacks
 */
@ControllerAdvice(annotations = RequestMapping.class)
public class JsonpAdvice extends AbstractJsonpResponseBodyAdvice {

    @Autowired
    public JsonpAdvice(@Value("${jsonp.callback:callback}") String callbackFunctionName) {
        super(callbackFunctionName);
    }

}
