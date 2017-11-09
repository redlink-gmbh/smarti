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

package io.redlink.smarti.processing;

import io.redlink.nlp.api.content.StringContent;
import io.redlink.nlp.api.model.Annotation;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.AnalyzedText.AnalyzedTextBuilder;
import io.redlink.nlp.model.Section;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import static io.redlink.smarti.processing.SmartiAnnotations.*;

public class ProcessingData extends io.redlink.nlp.api.ProcessingData {
    
    protected ProcessingData(Conversation conversation, AnalyzedText at) {
        super(new StringContent(at.getText()), null);
        addAnnotation(AnalyzedText.ANNOTATION, at);
        addAnnotation(CONVERSATION_ANNOTATION, conversation);
    }
    
    public static ProcessingData create(Conversation conversation){
        AnalyzedTextBuilder atb = AnalyzedText.build();
        int numMessages = conversation.getMessages().size();
        boolean first = true;
        for(int i=0;i < numMessages; i++){
            Message message = conversation.getMessages().get(i);
            if(StringUtils.isNoneBlank(message.getContent())){
                Section section = atb.appendSection(first ? null : "\n", message.getContent(), "\n");
                section.addAnnotation(MESSAGE_IDX_ANNOTATION, i);
                section.addAnnotation(MESSAGE_ANNOTATION, message);
                first = false;
            } //else ignore blank messages for analysis
        }
        return new ProcessingData(conversation, atb.create());
    }
    /**
     * Shorthand for {@link #getAnnotation(Annotation)} with {@link #CONVERSATION_ANNOTATION}
     * @return the Conversation for this {@link ProcessingData}
     */
    public final Conversation getConversation(){
        return getAnnotation(CONVERSATION_ANNOTATION);
    }
    
    /**
     * Shorthand for {@link #getAnnotation(Annotation)} with {@link AnalyzedText#ANNOTATION}
     * @return the {@link AnalyzedText} over the messages of the Conversations.
     */
    public final AnalyzedText getAnalyzedText(){
        return getAnnotation(AnalyzedText.ANNOTATION);
    }
    /**
     * List over the {@link Section}s for {@link Message}s of the {@link Conversation}
     * @return
     */
    public final List<Section> getMessageSections(){
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(getAnalyzedText().getSections(),Spliterator.ORDERED),false)
            .filter(s -> s.getAnnotation(MESSAGE_ANNOTATION) != null)
            .collect(Collectors.toList());
    }
    
}
