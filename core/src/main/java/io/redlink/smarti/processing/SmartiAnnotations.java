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

import io.redlink.nlp.api.model.Annotation;
import io.redlink.nlp.model.Section;
import io.redlink.smarti.model.Analysis;
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

    public final static Annotation<Analysis> ANALYSIS_ANNOTATION  = new Annotation<>(
            "io_redlink_smarti_annotation_analysis", Analysis.class);

}
