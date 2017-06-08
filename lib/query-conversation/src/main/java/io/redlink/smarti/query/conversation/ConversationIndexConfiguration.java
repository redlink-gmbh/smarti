package io.redlink.smarti.query.conversation;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;

@Configuration
public class ConversationIndexConfiguration {
    
    public final static String CONVERSATION_INDEX = "conversation";
    
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_DOMAIN = "domain";
    public static final String FIELD_MESSAGE_COUNT = "message_count";
    public static final String FIELD_START_TIME = "start_time";
    public static final String FIELD_END_TIME = "end_time";
    public static final String FIELD_CONVERSATION_ID = "conversation_id";
    public static final String FIELD_MESSAGE_ID = "message_id";
    public static final String FIELD_MESSAGE_IDX = "message_idx";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_VOTE = "vote";
    /**
     * Field containing interesting terms configured for MLT queries
     */
    public static final String FIELD_INTERESTING_TERMS = "iterms";
    
    public static final String FIELD_MODIFIED = "modified";
    
    
    @Bean(name=CONVERSATION_INDEX)
    protected SolrCoreDescriptor getConversationCoreDescriptor() throws IOException {
        return SimpleCoreDescriptor.createFromResource(CONVERSATION_INDEX, "/solr/core/" + CONVERSATION_INDEX, ConversationIndexConfiguration.class);
    }
    
    
}
