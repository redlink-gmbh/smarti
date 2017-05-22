/*
 * Copyright (c) 2016 Redlink GmbH
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

/**
 */
@Component
public class ProductTemplate extends QueryTemplateDefinition {

    public static final String PRODUCT = "product";
    public static final String WHAT = "what";

    @Autowired
    private SpeakService speakService;

    public ProductTemplate() {
        super(MessageTopic.Produkt);
    }

    @Override
    protected QuerySlot createSlotForName(String name) {
        switch (name) {
            case PRODUCT:
                return new QuerySlot(PRODUCT, Token.Type.Product,
                        speakService.getMessage("slot.product."+PRODUCT, "Was möchtest Du wissen?"), true);
            case WHAT:
                return new QuerySlot(WHAT, null,
                        speakService.getMessage("slot.product."+WHAT, "Was suchst Du genau?"), false); //Types: Train, Attribute
            default:
                log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
                return null; //unknown slot
        }
    }

    @Override
    protected boolean validate(Collection<QuerySlot> slots, List<Token> tokens) {
        final Set<String> present = getPresentAndValidSlots(slots, tokens);
        return present.contains(PRODUCT);
    }
}
