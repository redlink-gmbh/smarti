package io.redlink.smarti.query.dbsearch;

import static io.redlink.smarti.query.dbsearch.DbSearchTemplateDefinition.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    
    private static final Set<Type> ENTITY_TYPES = EnumSet.of(Type.Entity, Type.Place, Type.Person, Type.Organization,Type.Product);
    
    @Override
    protected TemplateDefinition getDefinition() {
        return DB_SEARCH_TEMPLATE_DEF;
    }

    @Override
    protected Set<Integer> updateTemplate(Template template, Conversation conversation, int startMsgIdx) {
        if(template.getState() == State.Confirmed || template.getState() == State.Rejected){
            return null; //do not update this template
        }
        //map used to avoid adding multiple slots with the same Role pointing to a Token with the same value
        Map<String, Map<String, Slot>> roleNameSlots = new HashMap<>();
        template.getSlots().stream()
            .filter(s -> s.getTokenIndex() >= 0)
            .forEach(s -> addSlot(roleNameSlots, conversation, s));
        
        
        Set<Integer> usedTokenIdxs = template.getSlots().stream()
                .filter(s -> s.getTokenIndex() >= 0)
                .map(Slot::getTokenIndex).collect(Collectors.toSet());
        Set<Integer> updatedIdxs = new HashSet<>();
        for(int i=0;i<conversation.getTokens().size();i++){
            Token t = conversation.getTokens().get(i);
            if(t.getMessageIdx() >= startMsgIdx && !usedTokenIdxs.contains(i)){
                final Slot slot;
                if(t.getType() == Token.Type.Keyword){
                    slot = DB_SEARCH_TEMPLATE_DEF.createSlot(ROLE_KEYWORD);
                } else if(t.getType() == Token.Type.Term){
                    slot = DB_SEARCH_TEMPLATE_DEF.createSlot(ROLE_TERM);
                } else if(t.getType() == Token.Type.Topic){
                    slot = DB_SEARCH_TEMPLATE_DEF.createSlot(ROLE_TOPIC);
                } else if(ENTITY_TYPES.contains(t.getType())){
                    slot = DB_SEARCH_TEMPLATE_DEF.createSlot(ROLE_ENTITY);
                } else {
                    slot = null;
                }
                if(slot != null){
                    slot.setTokenIndex(i);
                    if(addSlot(roleNameSlots, conversation, slot)){
                        template.getSlots().add(slot);
                        updatedIdxs.add(i);
                    }
                }
            }
        }
        return updatedIdxs;
    }

    private boolean addSlot(Map<String, Map<String, Slot>> roleNameSlots, Conversation conversation, Slot slot) {
        Map<String, Slot> roleSlots = roleNameSlots.get(slot.getRole());
        Token t = conversation.getTokens().get(slot.getTokenIndex());
        if(t.getValue() != null){
            if(roleSlots == null){
                roleSlots = new HashMap<>();
                roleNameSlots.put(slot.getRole(), roleSlots);
            }
            String value = t.getValue().toString();
            Slot present = roleSlots.get(value);
            if(present == null){
                roleSlots.put(value, slot);
                return true;
            } else {
                Token presentToken = conversation.getTokens().get(present.getTokenIndex());
                if(presentToken.getConfidence() < t.getConfidence()){
                    roleSlots.put(value, slot);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void initializeTemplate(Template queryTemplate) {
        //no op
    }

}
