/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.intend;

import io.redlink.smarti.model.IntendDefinition;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.services.SpeakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 */
@Component
public class TrainInfoIntendDefinition extends IntendDefinition {

    public static final String TRAIN = "train";
    public static final String DATE = "date";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String WHAT = "what";

    @Autowired
    private SpeakService speakService;

    public TrainInfoIntendDefinition() {
        super(MessageTopic.Zuginformation);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
            case TRAIN:
                return new Slot(TRAIN, Token.Type.Train,
                        speakService.getMessage("slot.traininfo."+TRAIN, "Um welchen Zug geht es?"), false);
            case DATE:
                return new Slot(DATE, Token.Type.Date,
                        speakService.getMessage("slot.traininfo."+DATE, "Geht es um den aktuellen Zug?"), true);
            case FROM:
                return new Slot(FROM, Token.Type.Place,
                        speakService.getMessage("slot.traininfo."+FROM, "Wo ist der Zug abgefahren?"), false);
            case TO:
                return new Slot(TO, Token.Type.Place,
                        speakService.getMessage("slot.traininfo."+TO, "Wohin fährt der Zug?"), false);
            case WHAT:
                return new Slot(WHAT, null,
                        speakService.getMessage("slot.traininfo."+WHAT, "Was suchst Du genau?"), false);
            default:
                log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
                return null; //unknown slot
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        final Set<String> present = getPresentAndValidSlots(slots, tokens);

        return present.contains(DATE) &&
                (present.contains(TRAIN) || present.containsAll(Arrays.asList(FROM, TO)));
    }
}
