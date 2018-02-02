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

package io.redlink.smarti.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.Map;

@ResponseStatus(code=HttpStatus.BAD_REQUEST)
public class BadArgumentException extends IllegalArgumentException implements DataException<Map<String,Object>>{

    private static final long serialVersionUID = -2487599342423211225L;

    private final Map<String,Object> data;
    
    public BadArgumentException(String field, Object value) {
        this(field,value,null);
    }
    
    public BadArgumentException(String field, Object value, String message) {
        super(value == null ? "NULL is not supported as value for field "+ field :
            "Unsupported value for '"+field+"' (value: '"+value+"'"
                + (message != null ? ", message: " + message : "") + ")!");
        data = Collections.singletonMap(field, message);
    }

    public BadArgumentException(String field, Class<?> parsed, Class<?> expected) {
        this(field, parsed, expected, null);
    }
    public BadArgumentException(String field, Class<?> parsed, Class<?> expected,  String message) {
        super("Unexpected value of type " + parsed.getName() + " for field '" + field
                + "'(expected type : " + expected.getName()
                + (message != null ? ", message: " + message : "") + ")!");
        data = Collections.singletonMap(field, message);
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }
    
}
