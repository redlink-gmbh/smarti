/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.query.template;

import io.redlink.smarti.model.MessageTopic;
import io.redlink.smarti.model.QuerySlot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.query.QueryTemplateDefinition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * QueryTemplate for {@link io.redlink.smarti.model.MessageTopic#Sonstiges}.
 */
@Component
public class OtherTemplate extends QueryTemplateDefinition {

    public OtherTemplate() {
        super(MessageTopic.Sonstiges);
    }

    @Override
    protected QuerySlot createSlotForName(String name) {
        return null;
    }

    @Override
    protected boolean validate(Collection<QuerySlot> slots, List<Token> tokens) {
        return true;
    }
}
