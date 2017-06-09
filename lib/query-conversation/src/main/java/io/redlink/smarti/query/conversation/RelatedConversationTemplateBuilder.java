package io.redlink.smarti.query.conversation;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Template;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;
import io.redlink.smarti.model.Token.Type;

@Component
public class RelatedConversationTemplateBuilder extends TemplateBuilder {

    public static final TemplateDefinition RELATED_CONV_INTEND_DEF = new RelatedConversationTemplateDefinition();
    
    @Override
    protected TemplateDefinition getDefinition() {
        return RELATED_CONV_INTEND_DEF;
    }

    @Override
    protected Set<Integer> updateTemplate(Template template, Conversation conversation, int startMsgIdx) {
        Set<Integer> usedTokenIdxs = template.getSlots().stream()
                .filter(s -> s.getTokenIndex() >= 0)
                .map(Slot::getTokenIndex).collect(Collectors.toSet());
        Set<Integer> updatedIdxs = new HashSet<>();
        for(int i=0;i<conversation.getTokens().size();i++){
            Token t = conversation.getTokens().get(i);
            if(t.getMessageIdx() >= startMsgIdx && !usedTokenIdxs.contains(i)){
                Slot slot = null;
                if(t.getType() == Type.Keyword){
                    slot = RELATED_CONV_INTEND_DEF.createSlot(RelatedConversationTemplateDefinition.ROLE_KEYWORD);
                } else if(t.getType() != Type.Topic){
                    slot = RELATED_CONV_INTEND_DEF.createSlot(RelatedConversationTemplateDefinition.ROLE_TERM);
                }
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
