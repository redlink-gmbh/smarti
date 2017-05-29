package io.redlink.smarti.utils;

import org.bson.types.ObjectId;

import io.redlink.smarti.exception.BadArgumentException;

public class ModelUtils {

    private ModelUtils() {
        throw new IllegalStateException("Do not use reflation to create instances of Utility classes");
    }

    /**
     * parses an {@link ObjectId} from the parsed String id.
     * @param id the id
     * @return the ObjectId or <code>null</code> if <code>null</code> is parsed as ID
     * @throws BadArgumentException if the parsed id is not a valid {@link ObjectId}
     */
    public static ObjectId parseObjectId(String id) {
        return parseObjectId(id, "id");
    }
    /**
     * parses an {@link ObjectId} from the parsed String id.
     * @param id the id
     * @param field the Field reported when throwing a {@link BadArgumentException}
     * @return the ObjectId or <code>null</code> if <code>null</code> is parsed as ID
     * @throws BadArgumentException if the parsed id is not a valid {@link ObjectId}
     */
    public static ObjectId parseObjectId(String id, String field) {
        if(id == null){
            return null;
        }
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new BadArgumentException(field, id, e.getMessage());
        }
    }

}
