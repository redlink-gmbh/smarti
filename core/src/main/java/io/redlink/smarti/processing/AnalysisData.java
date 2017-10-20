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
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.redlink.smarti.processing.SmartiAnnotations.*;

public class AnalysisData extends io.redlink.nlp.api.ProcessingData {
    
    protected AnalysisData(Conversation conversation, Analysis analysis, AnalyzedText at) {
        super(new StringContent(at.getText()), null);
        addAnnotation(AnalyzedText.ANNOTATION, at);
        addAnnotation(CONVERSATION_ANNOTATION, conversation);
        addAnnotation(ANALYSIS_ANNOTATION, analysis);
    }
    
    public static AnalysisData create(Conversation conversation){
        return create(conversation, null);
    }
    
    public static AnalysisData create(Conversation conversation, Analysis analysis){
        AnalyzedTextBuilder atb = AnalyzedText.build();
        int numMessages = conversation.getMessages().size();
        for(int i=0;i < numMessages; i++){
            Message message = conversation.getMessages().get(i);
            Section section = atb.appendSection(i > 0 ? "\n" : null, message.getContent(), "\n");
            section.addAnnotation(MESSAGE_IDX_ANNOTATION, i);
            section.addAnnotation(MESSAGE_ANNOTATION, message);
        }
        return new AnalysisData(conversation, analysis, atb.create());
    }
    /**
     * Shorthand for {@link #getAnnotation(Annotation)} with {@link SmartiAnnotations#CONVERSATION_ANNOTATION}
     * @return the Conversation for this {@link AnalysisData}
     */
    public final Conversation getConversation(){
        return getAnnotation(CONVERSATION_ANNOTATION);
    }
    /**
     * Shorthand for {@link #getAnnotation(Annotation)} with {@link SmartiAnnotations#ANALYSIS_ANNOTATION}
     * @return the Analysis for this {@link AnalysisData}
     */
    public final Analysis getAnalysis(){
        return getAnnotation(ANALYSIS_ANNOTATION);
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
