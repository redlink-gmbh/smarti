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

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_INTERESTING_TERMS;

@Component
public final class ConversationInterestingTermsExtractor extends InterestingTermExtractor {

    private final SolrCoreContainer solrServer;
    private final SolrCoreDescriptor conversationCore;
    
    private final MltConfig mltConfig;

    @Autowired
    public ConversationInterestingTermsExtractor(SolrCoreContainer solrServer, 
            @Qualifier(ConversationIndexConfiguration.CONVERSATION_INDEX) SolrCoreDescriptor conversationCore){
        super(conversationCore.getCoreName());
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
        this.mltConfig = MltConfig.getDefault();
        //TODO: multi lingual support .. currently conversations are indexed with a German language configuration
        mltConfig.setSimilarityFields("de", Arrays.asList(FIELD_INTERESTING_TERMS));
    }
    
    @Override
    protected SolrClient getClient() throws SolrServerException {
        return solrServer.getSolrClient(conversationCore);
    }
    
    @Override
    protected MltConfig getMltConf() {
        return mltConfig;
    }
}
