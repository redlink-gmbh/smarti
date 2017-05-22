/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.processing;

import io.redlink.nlp.model.*;
import io.redlink.nlp.model.AnalyzedText.AnalyzedTextBuilder;
import io.redlink.nlp.model.keyword.Keyword;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

/**
 * Class used to store all information related to
 * processing requests for a {@link Conversation}
 * @author Rupert Westenthaler
 *
 */
public class ProcessingData {

    /**
     * Used to Annotation {@link Section}s with the index of the Message
     * within {@link Conversation#getMessages()} they represent.
     */
    public final static Annotation<Integer> MESSAGE_IDX_ANNOTATION = new Annotation<>(
            "io_redlink_reisebuddy_annotation_message_idx", Integer.class);
    /**
     * Used to Annotation {@link Section}s with the index of the Message
     * within {@link Conversation#getMessages()} they represent.
     */
    public final static Annotation<Message> MESSAGE_ANNOTATION = new Annotation<>(
            "io_redlink_reisebuddy_annotation_message", Message.class);
    
    /**
     * @deprecated use {@link Annotations#KEYWORD_ANNOTATION}
     */
    public final static Annotation<Keyword> KEYWORD_ANNOTATION = Annotations.KEYWORD_ANNOTATION;
    /**
     * Used to Annotate that a Token is negated.
     * @deprecated use {@link Annotations#NEGATION_ANNOTATION}
     */
    public final static Annotation<Boolean> NEGATION_ANNOTATION = Annotations.NEGATION_ANNOTATION;

    private final Conversation conversation;
    private final AnalyzedText analyzedText;
    
    public ProcessingData(Conversation conversation) {
        assert conversation != null;
        this.conversation = conversation;
        //build an Analyzed Text with Sections for the Messages in the
        //conversation
        AnalyzedTextBuilder atb = AnalyzedText.build();
        int numMessages = conversation.getMessages().size();
        for(int i=0;i < numMessages; i++){
            Message message = conversation.getMessages().get(i);
            Section section = atb.appendSection(i > 0 ? "\n" : null, message.getContent(), "\n");
            section.addAnnotation(MESSAGE_IDX_ANNOTATION, Value.value(i));
            section.addAnnotation(MESSAGE_ANNOTATION, Value.value(message));
        }
        this.analyzedText = atb.create();
    }
    
    public Conversation getConversation() {
        return conversation;
    }

    public AnalyzedText getAnalyzedText() {
        return analyzedText;
    }

    public String getLanguage() {
        // TODO hardcoded to "de" for now
        return "de";
    }
    
}
