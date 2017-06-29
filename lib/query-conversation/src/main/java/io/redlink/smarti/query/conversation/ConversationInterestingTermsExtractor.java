package io.redlink.smarti.query.conversation;

import static io.redlink.smarti.query.conversation.ConversationIndexConfiguration.FIELD_INTERESTING_TERMS;

import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

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
