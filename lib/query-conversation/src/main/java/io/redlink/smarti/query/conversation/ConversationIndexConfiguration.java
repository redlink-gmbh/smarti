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

package io.redlink.smarti.query.conversation;

import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ConversationIndexConfiguration {
    
    public final static String CONVERSATION_INDEX = "conversation";
    
    /**
     * The current conversation index version. Needs to be increased on schema.xml
     * or software updates that do require a full re-index
     */
    public static final int CONVERSATION_INDEX_VERSION = 5; //v5 to fix #207
    
    public static final String FIELD_ID = "id";
    /**
     * Field used to store the current {@link #CONVERSATION_INDEX_VERSION} so that
     * updates that require a full re-index can be detected on startup
     */
    public static final String FIELD_INDEX_VERSION = "index_version";
    public static final String FIELD_COMPLETED = "completed";
    public static final String FIELD_TYPE = "type";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_CONVERSATION = "conversation";
    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_USER_NAME = "user_name";
    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_DOMAIN = "domain";
    public static final String FIELD_MESSAGE_COUNT = "message_count";
    public static final String FIELD_START_TIME = "start_time";
    public static final String FIELD_END_TIME = "end_time";
    public static final String FIELD_CONVERSATION_ID = "conversation_id";
    public static final String FIELD_MESSAGE_IDS = "message_ids";
    public static final String FIELD_MESSAGE_IDXS = "message_idxs";
    
    public static final String FIELD_SYNC_DATE = "sync_date";
    /**
     * The conent of a message (single valued)
     */
    public static final String FIELD_MESSAGE = "message";
    /**
     * The contents of all messages of a conversation (multi valued)
     */
    public static final String FIELD_MESSAGES = "messages";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_VOTE = "vote";
    /**
     * Field containing interesting terms configured for MLT queries
     */
    public static final String FIELD_INTERESTING_TERMS = "iterms";
    
    public static final String FIELD_MODIFIED = "modified";
    
    
    private static final String ENV_FIELD_PREFIX = "env_";
    
    public static String getEnvironmentField(String key) {
        return ENV_FIELD_PREFIX + key;
    }

    private static final String META_FIELD_PREFIX = "meta_";

    public static String getMetaField(String key) {
        return META_FIELD_PREFIX + key;
    }
    
    
    @Bean(name=CONVERSATION_INDEX)
    protected SolrCoreDescriptor getConversationCoreDescriptor() throws IOException {
        return SimpleCoreDescriptor.createFromResource(CONVERSATION_INDEX, "/solr/core/" + CONVERSATION_INDEX, ConversationIndexConfiguration.class);
    }
    
    
}
