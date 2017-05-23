/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.intend;

import io.redlink.smarti.model.IntendDefinition;
import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * QueryTemplate for {@link io.redlink.smarti.model.MessageTopic#Sonstiges}.
 */
@Component
public class OtherIntendDefinition extends IntendDefinition {

    public OtherIntendDefinition() {
        super(MessageTopic.Sonstiges);
    }

    @Override
    protected Slot createSlotForName(String name) {
        return null;
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return true;
    }
}
