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

package io.redlink.smarti.query.solr;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.intend.IrLatchTemplate;
import io.redlink.smarti.model.*;
import io.redlink.smarti.model.Token.Type;
import org.springframework.stereotype.Component;

import static io.redlink.smarti.intend.IrLatchTemplate.*;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LatchTemplateBuilder extends TemplateBuilder {

    public static final TemplateDefinition LATCH = new IrLatchTemplate();
    
    private static final Set<Type> ENTITY_TYPES = EnumSet.of(Type.Entity, Type.Place, Type.Person, Type.Organization,Type.Product);
    
    @Override
    protected TemplateDefinition getDefinition() {
        return LATCH;
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
        //map used to avoid adding multiple slots with the same Role pointing to a Token with the same value
        Map<String, Map<String, Slot>> roleNameSlots = new HashMap<>();
        template.getSlots().stream()
            .filter(s -> s.getTokenIndex() >= 0)
            .forEach(s -> addSlot(roleNameSlots, analysis, s));
        
        
        Set<Integer> usedTokenIdxs = template.getSlots().stream()
                .filter(s -> s.getTokenIndex() >= 0)
                .map(Slot::getTokenIndex).collect(Collectors.toSet());
        
        Set<Integer> updatedIdxs = new HashSet<>();
        
        for(int i=0;i<analysis.getTokens().size();i++){
            Token t = analysis.getTokens().get(i);
            if(t.getMessageIdx() >= startMsgIdx && !usedTokenIdxs.contains(i)){
                final Slot slot;
                if(t.getType() == Token.Type.Place){
                    slot = LATCH.createSlot(ROLE_LOCATION);
                } else if(t.getType() == Token.Type.Date){
                    slot = LATCH.createSlot(ROLE_TIME);
                } else if(t.getType() == Token.Type.Topic){
                    slot = LATCH.createSlot(ROLE_CATEGORY);
                } else if(t.getType() == Token.Type.Entity ||
                        t.getType() == Token.Type.Other ||
                        t.getType() == Token.Type.Person ||
                        t.getType() == Token.Type.Product ||
                        t.getType() == Token.Type.Organization ||
                        t.getType() == Token.Type.Keyword){
                    slot = LATCH.createSlot(ROLE_ALPHABET);
                //} else if( //TODO: how to identify hierarchy terms
                //    slot = LATCH.createSlot(ROLE_HIERARCHY);
                } else {
                    slot = null;
                }
                if(slot != null){
                    slot.setTokenIndex(i);
                    if(addSlot(roleNameSlots, analysis, slot)){
                        template.getSlots().add(slot);
                        updatedIdxs.add(i);
                    }
                }
            }
        }
        return updatedIdxs;
    }

    private boolean addSlot(Map<String, Map<String, Slot>> roleNameSlots, Analysis analysis, Slot slot) {
        Map<String, Slot> roleSlots = roleNameSlots.get(slot.getRole());
        Token t = analysis.getTokens().get(slot.getTokenIndex());
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
                Token presentToken = analysis.getTokens().get(present.getTokenIndex());
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
