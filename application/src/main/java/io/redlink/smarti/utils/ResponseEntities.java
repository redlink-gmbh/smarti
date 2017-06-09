/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ResponseEntities {

    public static ResponseEntityBuilder build(HttpStatus status){
        return new ResponseEntityBuilder(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status);
    }
    public static ResponseEntityBuilder build(int status){
        return new ResponseEntityBuilder(status);
    }
    
    public static ResponseEntity<Map<String,Object>> status(HttpStatus status, String message) {
        return build(status).message(message).build();
    }

    public static ResponseEntity<Map<String,Object>> status(int status, String message) {
        return build(status).message(message).build();
    }

    public static ResponseEntity<Map<String,Object>> badRequest(String message) {
        return status(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseEntity<Map<String,Object>> internalServerError(String message) {
        return status(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ResponseEntity<?> internalServerError(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR).message(e.getMessage()).data(e).build();
    }

    public static ResponseEntity<?> serviceUnavailable(String message) {
        return status(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public static ResponseEntity<?> serviceUnavailable(String message, Object data) {
        return build(HttpStatus.SERVICE_UNAVAILABLE).message(message).data(data).build();
    }

    public static ResponseEntity<?> unprocessableEntity(String message) {
        return status(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static ResponseEntity<Map<String,Object>> conflict(String message) {
        return status(HttpStatus.CONFLICT, message);
    }

    public static ResponseEntity<?> notImplemented() {
        return status(HttpStatus.NOT_IMPLEMENTED, "Not (yet) implemented");
    }

    public static class ResponseEntityBuilder {
        
        private final int status;
        private final Map<String, Object> data = new HashMap<>();
        
        private ResponseEntityBuilder(HttpStatus status){
            this(status == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : status.value());
        }
        
        private ResponseEntityBuilder(int status){
            assert status >= 100 && status < 600;
            this.status = status;
            this.data.put("status", status);
        }
        
        public ResponseEntityBuilder message(String message){
            if(StringUtils.isBlank(message)){
                data.remove("message");
            } else {
                data.put("message", message);
            }
            return this;
        }
        
        public ResponseEntityBuilder code(int errorCode){
            data.put("code", errorCode);
            return this;
        }
        
        public ResponseEntityBuilder data(Object data){
            if(data != null){
                this.data.put("data", data);
            }
            return this;
        }
        
        public ResponseEntityBuilder trace(String trace){
            if(StringUtils.isBlank(trace)){
                data.remove("trace");
            } else {
                data.put("trace", trace);
            }
            return this;
        }
        
        public ResponseEntity<Map<String,Object>> build(){
            return ResponseEntity.status(status).body(data);
        }
    }
    
}
