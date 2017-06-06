package io.redlink.smarti.exception;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code=HttpStatus.BAD_REQUEST)
public class BadArgumentException extends IllegalArgumentException implements DataException<Map<String,String>>{

    private static final long serialVersionUID = -2487599342423211225L;

    private final Map<String,String> data;
    
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
    public Map<String, String> getData() {
        return data;
    }
    
}
