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

package io.redlink.smarti.chatpal.index;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.redlink.smarti.chatpal.model.ChatpalMessage;
import io.redlink.solrlib.SimpleCoreDescriptor;
import io.redlink.solrlib.SolrCoreDescriptor;

@Configuration
public class ChatpalIndexConfiguration {
    
    public final static String CHATPAL_INDEX = "chatpal";
    
    public final static String SMARTI_FIELD_PREFIX = "smarti_";
    
    public static final int INDEX_VERSION = 1; //increase after a schema change in the index
    
    public final static String FIELD_CLIENT = SMARTI_FIELD_PREFIX + "client";
    /**
     * The field used to store the {@link ChatpalMessage#getId()} value
     */
    public static final String FIELD_ID = SMARTI_FIELD_PREFIX + "id";
    /**
     * Field used to store the current {@link #CONVERSATION_INDEX_VERSION} so that
     * updates that require a full re-index can be detected on startup
     */
    public static final String FIELD_INDEX_VERSION = SMARTI_FIELD_PREFIX + "index_version";
    public static final String FIELD_SYNC_DATE = SMARTI_FIELD_PREFIX + "sync_date";

    
    @Bean(name=CHATPAL_INDEX)
    protected SolrCoreDescriptor getConversationCoreDescriptor() throws IOException {
        return SimpleCoreDescriptor.createFromResource(CHATPAL_INDEX, "/solr/core/" + CHATPAL_INDEX, ChatpalIndexConfiguration.class);
    }
    
    
}
