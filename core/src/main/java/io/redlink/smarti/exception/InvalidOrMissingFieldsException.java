package io.redlink.smarti.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code=HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidOrMissingFieldsException extends RuntimeException implements DataException<Map<String,Object>>{

    private static final long serialVersionUID = 6594209986604095925L;

    private final Class<?> entityClass;
    private final Map<String,String> illegal;
    private final Set<String> missing;

    public InvalidOrMissingFieldsException(Class<?> entityClass, Map<String,String> illegal, Set<String> missing) {
        super("Unable to process " + entityClass.getName() + " because of " + (illegal == null ? "no" : illegal.size())
                + " illegal "+(illegal != null ? illegal.keySet().toString() : "") 
                + " and " + (missing == null ? "no" : missing.size()) + " missing fields "
                + (missing != null ? missing.toString() : ""));
        this.entityClass = entityClass;
        this.illegal = illegal == null ? Collections.emptyMap() : Collections.unmodifiableMap(illegal);
        this.missing = missing == null ? Collections.emptySet() : Collections.unmodifiableSet(missing);
    }
    
    public Class<?> getEntityClass() {
        return entityClass;
    }
    
    public Map<String, String> getIllegal() {
        return illegal;
    }
    
    public Set<String> getMissing() {
        return missing;
    }

    @Override
    public Map<String, Object> getData() {
        Map<String,Object> data = new HashMap<>();
        if(!illegal.isEmpty()){
            data.put("illegal", illegal);
        }
        if(!missing.isEmpty()){
            data.put("missing", missing);
        }
        return data.isEmpty() ? null : data;
    }
    
}
