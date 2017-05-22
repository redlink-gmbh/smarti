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
public class TravelTemplate extends QueryTemplateDefinition {

    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String VIA = "via";
    public static final String DEPART = "depart";
    public static final String ARRIVE = "arrive";
    public static final String CARD = "card";
    public static final String CLASS = "class";

    @Autowired
    private SpeakService speakService;

    public TravelTemplate() {
        super(MessageTopic.Reiseplanung);
    }
    
    @Override
    protected QuerySlot createSlotForName(String name) {
        switch (name) {
        case FROM:
            return new QuerySlot(FROM, Token.Type.Place,
                    speakService.getMessage("slot.travel."+FROM, "Wo möchtest Du abfahren?"), true);
        case TO:
            return new QuerySlot(TO, Token.Type.Place,
                    speakService.getMessage("slot.travel."+TO, "Wohin möchtest Du?"), true);
        case VIA:
            return new QuerySlot(VIA, Token.Type.Place,
                    speakService.getMessage("slot.travel."+VIA, ""), false);
        case DEPART:
            return new QuerySlot(DEPART, Token.Type.Date,
                    speakService.getMessage("slot.travel."+DEPART, "Wann willst Du los?"), false);
        case ARRIVE:
            return new QuerySlot(ARRIVE, Token.Type.Date,
                    speakService.getMessage("slot.travel."+ARRIVE, "Wann willst Du ankommen?"), false);
        case CARD:
            return new QuerySlot(CARD, Token.Type.Product,
                    speakService.getMessage("slot.travel."+CARD, "Hast Du eine Kundenkarte?"), false);
        case CLASS:
            return new QuerySlot(CLASS, Token.Type.Product,
                    speakService.getMessage("slot.travel."+CLASS, "Welcher Klasse möchtest Du reisen?"), false);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null; //unknown slot
        }
    }

    @Override
    protected boolean validate(Collection<QuerySlot> slots, List<Token> tokens) {
        final Set<String> present = getPresentAndValidSlots(slots, tokens);
        return present.contains(FROM) && present.contains(TO) &&
                (present.contains(DEPART) || present.contains(ARRIVE));
    }

}
