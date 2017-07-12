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

package io.redlink.smarti.utils;

import io.redlink.smarti.exception.BadArgumentException;
import org.bson.types.ObjectId;

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
