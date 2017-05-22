/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.query.template;

import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.QuerySlot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.query.QueryTemplateDefinition;
import io.redlink.smarti.services.SpeakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class PerimeterTemplate extends QueryTemplateDefinition {

    public static final String LOCATION = "location";
    public static final String START = "start";
    public static final String END = "end";
    public static final String WHAT = "what";

    @Autowired
    private SpeakService speakService;

    public PerimeterTemplate() {
        super(MessageTopic.Umkreissuche);
    }
    
    @Override
    protected QuerySlot createSlotForName(String name) {
        switch (name) {
        case LOCATION:
            return new QuerySlot(LOCATION, Token.Type.Place,
                    speakService.getMessage("slot.perimeter."+LOCATION, "Und wo genau?"), true);
        case START:
            return new QuerySlot(START, Token.Type.Date,
                    speakService.getMessage("slot.perimeter."+START, "Ab wann?"), false);
        case END:
            return new QuerySlot(END, Token.Type.Date,
                    speakService.getMessage("slot.perimeter."+END, "Bis wann?"), false);
        case WHAT:
            return new QuerySlot(WHAT, null,
                    speakService.getMessage("slot.perimeter."+WHAT, "Was genau suchst Du?"), true);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<QuerySlot> slots, List<Token> tokens) {
        final Set<String> present = getPresentAndValidSlots(slots, tokens);
        return present.contains(LOCATION) && present.contains(WHAT);
    }

}
