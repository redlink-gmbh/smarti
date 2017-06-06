package io.redlink.smarti.processing;

import static io.redlink.smarti.processing.SmartiAnnotations.*;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.redlink.nlp.api.content.StringContent;
import io.redlink.nlp.api.model.Annotation;
import io.redlink.nlp.model.AnalyzedText;
import io.redlink.nlp.model.Section;
import io.redlink.nlp.model.AnalyzedText.AnalyzedTextBuilder;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

public class ProcessingData extends io.redlink.nlp.api.ProcessingData {
    
    protected ProcessingData(Conversation conversation, AnalyzedText at) {
        super(new StringContent(at.getText()), null);
        addAnnotation(AnalyzedText.ANNOTATION, at);
        addAnnotation(CONVERSATION_ANNOTATION, conversation);
    }
    
    public static ProcessingData create(Conversation conversation){
        AnalyzedTextBuilder atb = AnalyzedText.build();
        int numMessages = conversation.getMessages().size();
        for(int i=0;i < numMessages; i++){
            Message message = conversation.getMessages().get(i);
            Section section = atb.appendSection(i > 0 ? "\n" : null, message.getContent(), "\n");
            section.addAnnotation(MESSAGE_IDX_ANNOTATION, i);
            section.addAnnotation(MESSAGE_ANNOTATION, message);
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
