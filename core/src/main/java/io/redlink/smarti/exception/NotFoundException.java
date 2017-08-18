package io.redlink.smarti.exception;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code=HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 7187297505896020792L;

    private Object entity;
    private final Class<?> type;
    private final Object id;

    public NotFoundException(Object entity, Object id) {
        this(entity.getClass(), id);
        this.entity = entity;
    }

    public NotFoundException(Class<?> type, Object id) {
        this(type,id,null);
    }

    public NotFoundException(Class<?> type, Object id, String message) {
        super(String.format("Unable to find %s with id %s%s",
                type.getSimpleName(),id, 
                StringUtils.isNoneBlank(message) ? " (message: " + message + ")" : ""));
        this.type = type;
        this.id = id;
    }
    /**
     * The entity (if available)
     * @return the entity (or <code>null</code>)
     */
    public Object getEntity() {
        return entity;
    }
    
    /**
     * The type of the entity
     * @return the type
     */
    public Class<?> getEntityType() {
        return type;
    }
    
    /**
     * The ID of the entity
     * @return the id
     */
    public Object getId() {
        return id;
    }
    
    
}
