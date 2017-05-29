package io.redlink.smarti.processing;

import io.redlink.nlp.api.model.Annotation;
import io.redlink.nlp.model.Section;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Message;

public interface SmartiAnnotations {
    /**
     * Used to Annotation {@link Section}s with the index of the Message
     * within {@link Conversation#getMessages()} they represent.
     */
    public final static Annotation<Integer> MESSAGE_IDX_ANNOTATION = new Annotation<>(
            "io_redlink_smarti_annotation_message_idx", Integer.class);
    /**
     * Used to Annotation {@link Section}s with the index of the Message
     * within {@link Conversation#getMessages()} they represent.
     */
    public final static Annotation<Message> MESSAGE_ANNOTATION = new Annotation<>(
            "io_redlink_smarti_annotation_message", Message.class);
    
    public final static Annotation<Conversation> CONVERSATION_ANNOTATION  = new Annotation<>(
            "io_redlink_smarti_annotation_conversation", Conversation.class);

}
