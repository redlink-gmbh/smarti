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

package io.redlink.smarti.intend;

import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Token;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * QueryTemplate definition for LATCH information retrieval
 * @author Rupert Westenthaler
 *
 */
@Component
public class LatchSearchTemplate extends TemplateDefinition {

    public static final String DBSEARCH_TYPE = "dbsearch";
    
    public static final String ROLE_LOCATION = "location";
    public static final String ROLE_ALPHABET = "alphabet";
    public static final String ROLE_TIME = "time";
    public static final String ROLE_CATEGORY = "category";
    /**
     * Things like price ranges, Rating ranges ...
     */
    public static final String ROLE_HIERARCHY = "hierarchy";
    
    public LatchSearchTemplate() {
        super(DBSEARCH_TYPE);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case ROLE_LOCATION:
            return new Slot(ROLE_LOCATION, Token.Type.Place);
        case ROLE_ALPHABET:
            return new Slot(ROLE_ALPHABET, null);
        case ROLE_TIME:
            return new Slot(ROLE_TIME, Token.Type.Date);
        case ROLE_CATEGORY:
            return new Slot(ROLE_CATEGORY, Token.Type.Topic);
        case ROLE_HIERARCHY:
            return new Slot(ROLE_HIERARCHY, null);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return slots.stream()
                .filter(s -> s.getTokenIndex() >= 0).findAny().isPresent();
    }

}
