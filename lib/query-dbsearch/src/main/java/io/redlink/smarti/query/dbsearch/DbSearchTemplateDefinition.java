package io.redlink.smarti.query.dbsearch;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

@Component
public class DbSearchTemplateDefinition extends TemplateDefinition {

    public static final String DBSEARCH_TYPE = "dbsearch";
    public static final String ROLE_TERM = "Term";
    
    public DbSearchTemplateDefinition() {
        super(DBSEARCH_TYPE);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case ROLE_TERM:
            return new Slot(ROLE_TERM, null);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return slots.stream()
                .filter(s -> s.getRole().equals(ROLE_TERM))
                .filter(s -> s.getTokenIndex() >= 0).findAny().isPresent();
    }

}
