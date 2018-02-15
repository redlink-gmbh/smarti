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

package io.redlink.smarti.api;

import io.redlink.smarti.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public abstract class TemplateBuilder {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The {@link TemplateDefinition} used for {@link Template}s build by this {@link TemplateBuilder} implementation
     * @return the query template definition. MUST NOT be <code>null</code>
     */
    protected abstract TemplateDefinition getDefinition();

    
//    protected abstract Set<MessageTopic> getSupportedTopics();

    /**
     * Updates an existing template based on the current state of the conversation
     * @param template the template to update
     * @param conversation the conversation
     * @param analysis the analysis results
     * @return the tokens used to fill slots of the template or <code>null</code> if the template was not updated
     */
    protected abstract Set<Integer> updateTemplate(Template template, Conversation conversation, Analysis analysis);

    protected abstract void initializeTemplate(Template template);

    /**
     * Builds and updates templates for the parsed conversation based on
     * tokens extracted starting from the parsed message index
     * @param conversation the conversation
     */
    public final void updateTemplate(Conversation conversation, Analysis analysis) {
//NOTE: Basically dead code as Analysis is now created from strech on any update of the conversation
//        //first check if we can update an existing query templates
//        for(Template template : analysis.getTemplates()){
//            if(getDefinition().getType().equals(template.getType())){// &&
//                    //TODO: Maybe we would like to update valid templates
//                    //!getDefinition().isValid(template, conversation.getTokens())){
//                Set<Integer> addedTokens = updateTemplate(template, conversation, analysis);
//                //TODO: check if this assumption is correct
//                if(addedTokens != null){
//                    return; //updated existing template ... do not build a new one (of the same type)
//                }
//            } //else not matching or already valid template
//        }

        //try to create a new Template
        final Template template = new Template(getDefinition().getType(), new HashSet<>());
        initializeTemplate(template);
        Set<Integer> addedTokensIdxs = updateTemplate(template, conversation, analysis);
        if(addedTokensIdxs != null){
            analysis.getTemplates().add(template);
        } //else do not create empty Templates

    }

    /**
     * Logs information about the query template
     * @param queryTemplate
     * @param tokens
     */
    protected final void debugQueryTemplate(Template queryTemplate, List<Token> tokens) {
        if(log.isDebugEnabled()){
            log.debug("Built QueryTemplate for {}",queryTemplate.getType());
            for(Slot slot : queryTemplate.getSlots()){
                log.debug(" - {}: {}",slot.getRole(), slot.getTokenIndex() < 0 ? 
                        String.format("unboud <%s> %s", slot.getTokenType(), slot.isRequired() ? "required" : "optional") : 
                            tokens.get(slot.getTokenIndex()));
            }
        }
    }

}
