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
import io.redlink.nlp.model.section.SectionTag;
import io.redlink.nlp.model.section.SectionType;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Analysis.AnalysisContext;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.redlink.nlp.model.NlpAnnotations.SECTION_ANNOTATION;
import static io.redlink.smarti.processing.SmartiAnnotations.*;

public class AnalysisData extends io.redlink.nlp.api.ProcessingData {
    
    private static final Logger log = LoggerFactory.getLogger(AnalysisData.class);
    
    protected AnalysisData(Conversation conversation, Analysis analysis, AnalyzedText at) {
        super(new StringContent(at.getText()), null);
        addAnnotation(AnalyzedText.ANNOTATION, at);
        addAnnotation(CONVERSATION_ANNOTATION, conversation);
        if(analysis != null){
            addAnnotation(ANALYSIS_ANNOTATION, analysis);
        }
    }
    
    public static AnalysisData create(Conversation conversation, Client client, MessageContentProcessor mcp){
        return create(conversation, client, mcp, -1);
    }
    public static AnalysisData create(Conversation conversation, Client client, MessageContentProcessor mcp, int contextSize){
        return create(conversation, new Analysis(client.getId(), conversation.getId(),conversation.getLastModified()), mcp, contextSize);
    }
    
    public static AnalysisData create(Conversation conversation, Analysis analysis, MessageContentProcessor mcp){
        return create(conversation, analysis, mcp,-1);
    }
    public static AnalysisData create(Conversation conversation, Analysis analysis, MessageContentProcessor mcp, int contextSize){
        AnalyzedTextBuilder atb = AnalyzedText.build();
        int numMessages = conversation.getMessages().size();
        boolean first = true;
        int startIdx = contextSize <= 0 ? 0 : Math.max(0, numMessages - contextSize);
        log.trace("analysisContext: [{}..{}](size: {})", startIdx, numMessages-1, contextSize);
        analysis.setContext(new AnalysisContext(startIdx, numMessages));
        int skipped = 0;
        for(int i=startIdx; i < numMessages; i++){
            Message message = conversation.getMessages().get(i);
            log.trace("message idx: {}", i);
            //#203: if the skipAnalysis attribute is set we do not analyse the content of this message
            boolean skipAnalysis = Boolean.parseBoolean(
                    Objects.toString(message.getMetadata().get(Message.Metadata.SKIP_ANALYSIS), "false"));
            log.trace("skip analysis: {}", skipAnalysis);
            if(!skipAnalysis){
                log.trace("message Content: {}", message.getContent());
                final String content;
                if(mcp == null){
                    content = message.getContent();
                } else {
                    content = mcp.processMessageContent(analysis.getClient(), conversation, message);
                    log.trace(" processed Content: {}", content);
                }
                if(StringUtils.isNotBlank(content)){
                    Section section = atb.appendSection(first ? null : "\n", content, "\n");
                    section.addAnnotation(MESSAGE_IDX_ANNOTATION, i);
                    section.addAnnotation(MESSAGE_ANNOTATION, message);
                    section.addAnnotation(SECTION_ANNOTATION, new SectionTag(SectionType.paragraph, "message"));
                    first = false;
                } //else ignore empty content
            } else { //else ignore messages marked as skipAnalysis
                skipped++;
            }
        }
        analysis.getContext().setSkipped(skipped);
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
