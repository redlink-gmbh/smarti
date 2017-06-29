package io.redlink.smarti.data.solrcore.crawl.systel;

import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.solrlib.SolrCoreContainer;
import io.redlink.solrlib.SolrCoreDescriptor;

@Component
@ConditionalOnBean(name=CrawlSystelIndexConfiguration.CRAWL_SYSTEL, value=SolrCoreDescriptor.class)
@AutoConfigureAfter(SolrCoreDescriptor.class)
public final class CrawlSystelInterestingTermsExtractor extends InterestingTermExtractor {

    private final SolrCoreContainer solrServer;
    private final SolrCoreDescriptor conversationCore;
    
    private final MltConfig mltConfig;

    @Autowired
    public CrawlSystelInterestingTermsExtractor(SolrCoreContainer solrServer, 
            @Qualifier(CrawlSystelIndexConfiguration.CRAWL_SYSTEL) SolrCoreDescriptor conversationCore){
        super("crawl.systel");
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
