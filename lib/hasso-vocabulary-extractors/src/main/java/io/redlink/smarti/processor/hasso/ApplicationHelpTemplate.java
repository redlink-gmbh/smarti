/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.processor.hasso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.redlink.smarti.model.IntendDefinition;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.services.SpeakService;

import java.util.Collection;
import java.util.List;

/**
 * Created by jakob on 02.09.16.
 */
@Component
public class ApplicationHelpTemplate extends IntendDefinition {

    public static final String SUPPORT_TYPE = "supportType";
    public static final String KEYWORD = "keyword";

    @Autowired
    private SpeakService speakService;

    public ApplicationHelpTemplate() {
        super(MessageTopic.ApplicationHelp);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
            case SUPPORT_TYPE:
                return new Slot(SUPPORT_TYPE, Token.Type.QuestionIdentifier,
                        speakService.getMessage("slot.applicationHelp."+SUPPORT_TYPE, "Was möchtest Du dazu wissen?"), true);
            case KEYWORD:
                return new Slot(KEYWORD, Token.Type.Keyword,
                        speakService.getMessage("slot.applicationHelp."+KEYWORD, "Worum geht es?"), false);
            default:
                return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return true;
    }
}
