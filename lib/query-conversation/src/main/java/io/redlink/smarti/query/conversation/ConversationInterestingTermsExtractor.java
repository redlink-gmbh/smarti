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

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_MESSAGE;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_OWNER;
import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_TYPE;

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
        mltConfig.setSimilarityFields("de", Arrays.asList(FIELD_MESSAGE));
        //we only want to consider single Messages (no conversations as they duplicate the content)
        mltConfig.setFilterQuery(String.format("%s:%s",FIELD_TYPE, ConversationIndexConfiguration.TYPE_MESSAGE));
    }
    
    @Override
    protected SolrClient getClient() throws SolrServerException {
        return solrServer.getSolrClient(conversationCore);
    }
    
    @Override
    protected MltConfig getMltConf() {
        return mltConfig;
    }
    
    /**
     * Ensures that the Conversations are filtered by the current client so that Suggested
     * Interesting Terms are only based on conversations owned by the current Client
     */
    @Override
    protected void beforeSimilarity(SolrQuery mltQuery, Analysis analysis, Conversation conversation) throws SimilarityNotSupportedException {
        if(analysis == null || analysis.getClient() == null){
            throw new SimilarityNotSupportedException("unknown client");
        }
        //ensure we only consider conversations of the current client
        mltQuery.addFilterQuery(String.format("%s:%s", FIELD_OWNER, analysis.getClient().toHexString()));
        //NOTE: SupportArea Filters could be done, but as it is currently not possible to configure
        //      the states (as Client specific Analysis Configuration is not available) we do not use
        //      this for now.
    }
}
