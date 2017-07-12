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

package io.redlink.smarti.query.dbsearch;

import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Token;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class DbSearchTemplateDefinition extends TemplateDefinition {

    public static final String DBSEARCH_TYPE = "dbsearch";
    
    public static final String ROLE_TERM = "Term";
    public static final String ROLE_ENTITY = "Entity";
    public static final String ROLE_KEYWORD = "Keyword";
    public static final String ROLE_TOPIC = "Topic";
    
    public DbSearchTemplateDefinition() {
        super(DBSEARCH_TYPE);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case ROLE_TERM:
            return new Slot(ROLE_TERM, Token.Type.Term);
        case ROLE_KEYWORD:
            return new Slot(ROLE_KEYWORD, Token.Type.Keyword);
        case ROLE_ENTITY:
            return new Slot(ROLE_ENTITY, null);
        case ROLE_TOPIC:
            return new Slot(ROLE_TOPIC, Token.Type.Topic);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return slots.stream()
                .filter(s -> s.getRole().equals(ROLE_TERM))
                .filter(s -> s.getTokenIndex() >= 0).findAny().isPresent();
    }

}
