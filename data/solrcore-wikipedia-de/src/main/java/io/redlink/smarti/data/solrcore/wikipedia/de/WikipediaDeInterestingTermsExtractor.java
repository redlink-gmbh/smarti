package io.redlink.smarti.data.solrcore.wikipedia.de;

import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

@Component
@ConditionalOnProperty(name="solrcore.wikipedia.de.resource")
public final class WikipediaDeInterestingTermsExtractor extends InterestingTermExtractor {

    private final SolrCoreContainer solrServer;
    private final SolrCoreDescriptor conversationCore;
    
    private final MltConfig mltConfig;

    @Autowired
    public WikipediaDeInterestingTermsExtractor(SolrCoreContainer solrServer, 
            @Qualifier(WikipediaDeIndexConfiguration.WIKIPEDIA_DE) SolrCoreDescriptor conversationCore){
        super("wikipedia.de");
        this.solrServer = solrServer;
        this.conversationCore = conversationCore;
        this.mltConfig = MltConfig.getDefault()
                .setMinDocFreq(1)
                .setMinTermFreq(1)
                .setMinWordLength(2) //ignore single letter words
                .setSimilarityFields("de", Arrays.asList("text"));
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
