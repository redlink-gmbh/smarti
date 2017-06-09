package io.redlink.smarti.query.conversation;

import java.util.Collection;
import java.util.List;

import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

public class RelatedConversationTemplateDefinition extends TemplateDefinition {

    public static final String ROLE_KEYWORD = "Keyword";
    public static final String ROLE_TERM = "Term";
    
    public RelatedConversationTemplateDefinition() {
        super("related.conversation");
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
