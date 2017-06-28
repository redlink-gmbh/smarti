package io.redlink.smarti.query.dbsearch;

import static io.redlink.smarti.query.dbsearch.DbSearchTemplateDefinition.ROLE_TERM;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.State;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

@Component
public class DbSearchTemplateBuilder extends TemplateBuilder {

    public static final TemplateDefinition DB_SEARCH_TEMPLATE_DEF = new DbSearchTemplateDefinition();
    
    private static final Set<Type> IGNORED_TYPES = EnumSet.of(Type.Topic, Type.Date);
    
    @Override
    protected TemplateDefinition getDefinition() {
        return DB_SEARCH_TEMPLATE_DEF;
    }

    @Override
    protected Set<Integer> updateTemplate(Template template, Conversation conversation, int startMsgIdx) {
        if(template.getState() == State.Confirmed || template.getState() == State.Rejected){
            return null; //do not update this template
        }
        Set<Integer> usedTokenIdxs = template.getSlots().stream()
                .filter(s -> s.getTokenIndex() >= 0)
                .map(Slot::getTokenIndex).collect(Collectors.toSet());
        Set<Integer> updatedIdxs = new HashSet<>();
        for(int i=0;i<conversation.getTokens().size();i++){
            Token t = conversation.getTokens().get(i);
            if(t.getMessageIdx() >= startMsgIdx && !usedTokenIdxs.contains(i)){
                Slot slot = null;
                if(t.getType() == null || !IGNORED_TYPES.contains(t.getType())){
                    slot = DB_SEARCH_TEMPLATE_DEF.createSlot(ROLE_TERM);
                } //else token with ignored type
                if(slot != null){
                    slot.setTokenIndex(i);
                    template.getSlots().add(slot);
                    updatedIdxs.add(i);
                }
            }
        }
        return updatedIdxs;
    }

    @Override
    protected void initializeTemplate(Template queryTemplate) {
        //no op
    }

}
