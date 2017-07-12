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

package io.redlink.smarti.query.conversation;

import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class RelatedConversationTemplateDefinition extends TemplateDefinition {

    public static final String RELATED_CONVERSATION_TYPE = "related.conversation";
    public static final String ROLE_KEYWORD = "Keyword";
    public static final String ROLE_TERM = "Term";
    
    public RelatedConversationTemplateDefinition() {
        super(RELATED_CONVERSATION_TYPE);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case ROLE_KEYWORD:
            return new Slot(ROLE_KEYWORD, Type.Keyword);
        case ROLE_TERM:
            return new Slot(ROLE_TERM, null);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return slots.stream().filter(s -> s.getTokenIndex() >= 0).findAny().isPresent();
    }

}
