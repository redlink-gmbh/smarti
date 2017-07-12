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

import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.services.SpeakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class PerimeterTemplateDefinition extends TemplateDefinition {

    public static final String LOCATION = "location";
    public static final String START = "start";
    public static final String END = "end";
    public static final String WHAT = "what";

    @Autowired
    private SpeakService speakService;

    public PerimeterTemplateDefinition() {
        super(MessageTopic.Umkreissuche.name());
    }
    
    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case LOCATION:
            return new Slot(LOCATION, Token.Type.Place,
                    speakService.getMessage("slot.perimeter."+LOCATION, "Und wo genau?"), true);
        case START:
            return new Slot(START, Token.Type.Date,
                    speakService.getMessage("slot.perimeter."+START, "Ab wann?"), false);
        case END:
            return new Slot(END, Token.Type.Date,
                    speakService.getMessage("slot.perimeter."+END, "Bis wann?"), false);
        case WHAT:
            return new Slot(WHAT, null,
                    speakService.getMessage("slot.perimeter."+WHAT, "Was genau suchst Du?"), true);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        final Set<String> present = getPresentAndValidSlots(slots, tokens);
        return present.contains(LOCATION) && present.contains(WHAT);
    }

}
