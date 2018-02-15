/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.query.conversation;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RelatedConversationTemplateBuilder extends TemplateBuilder {

    public static final TemplateDefinition RELATED_CONV_INTEND_DEF = new RelatedConversationTemplateDefinition();
    
    @Override
    protected TemplateDefinition getDefinition() {
        return RELATED_CONV_INTEND_DEF;
    }

    @Override
    protected Set<Integer> updateTemplate(Template template, Conversation conversation, Analysis analysis) {
        if(template.getState() == State.Confirmed || template.getState() == State.Rejected){
            return null; //do not update this template
        }
        //NOTE: startMsgIdx was used in the old API to tell TemplateBuilders where to start. As this might get (re)-
        //      added in the future (however in a different form) we set it to the default 0 (start from the beginning)
        //      to keep the code for now
        int startMsgIdx = 0;
        
        Set<Integer> usedTokenIdxs = template.getSlots().stream()
                .filter(s -> s.getTokenIndex() >= 0)
                .map(Slot::getTokenIndex).collect(Collectors.toSet());
        Set<Integer> updatedIdxs = new HashSet<>();
        for(int i=0;i<analysis.getTokens().size();i++){
            Token t = analysis.getTokens().get(i);
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
