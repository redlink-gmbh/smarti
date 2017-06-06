package io.redlink.smarti.exception;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code=HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException implements DataException<Map<String,String>>{

    private static final long serialVersionUID = -1519928397077278L;
    
    private final Map<String,String> conflicting;

    public ConflictException(Class<?> entityType, String field, String message) {
        this(entityType, Collections.singletonMap(field, message));
    }
    public ConflictException(Class<?> entityType, Map<String,String> conflicting) {
        super("Conflict with " + entityType.getSimpleName() +" (reasons: " + conflicting + ")");
        this.conflicting = conflicting;
    }
    
    @Override
    public Map<String, String> getData() {
        return conflicting;
    }
    
}
