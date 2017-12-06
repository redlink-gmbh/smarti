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
package io.redlink.smarti.webservice.pojo;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.redlink.smarti.exception.DataException;
import io.redlink.smarti.utils.ResponseEntities.ResponseEntityBuilder;
import io.swagger.annotations.ApiModel;

@ApiModel
public class CallbackPayload<T> {

    @JsonIgnore
    private final HttpStatus httpStatus;
    @JsonInclude(Include.NON_NULL)
    private final String message;
    @JsonInclude(Include.NON_NULL)
    private final T data;

    public static <T> CallbackPayload<T> success(T data){
        if(data == null){
            throw new NullPointerException();
        }
        return new CallbackPayload<T>(data);
    }
    
    public static CallbackPayload<Object> error(Throwable error){
        if(error == null){
            throw new NullPointerException();
        }
        final HttpStatus status;
        final String statusMessage;
        final Object data;
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(error.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            status = ObjectUtils.firstNonNull(responseStatus.code(), responseStatus.value());
            statusMessage = responseStatus.reason();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            statusMessage = null;
        }
        if(error instanceof DataException<?>){
            data = ((DataException<?>)error).getData();
        } else {
            data = null;
        }
        return new CallbackPayload<Object>(status, ObjectUtils.firstNonNull(error.getMessage(), statusMessage), data);
    }
    
    private CallbackPayload(T data) {
        this(HttpStatus.OK, null, data);
    }

    private CallbackPayload(HttpStatus status, String message, T data) {
        this.httpStatus = status;
        this.message = message; 
        this.data = data;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @JsonGetter
    public int getStatus(){
        return httpStatus.value();
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
