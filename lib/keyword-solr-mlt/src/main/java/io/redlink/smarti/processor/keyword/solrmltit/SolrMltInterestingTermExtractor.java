package io.redlink.smarti.processor.keyword.solrmltit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.smarti.processor.keyword.solrmltit.SolrMltInterestingTermConfiguration.SolrMltConfig;

public class SolrMltInterestingTermExtractor extends InterestingTermExtractor {

    private HttpSolrClient solrClient;
    private MltConfig mltConfig;

    public SolrMltInterestingTermExtractor(CloseableHttpClient httpClient, SolrMltConfig config) {
        super(config.getName());
        solrClient = new HttpSolrClient.Builder(config.getUrl()).withHttpClient(httpClient).build();
        mltConfig = MltConfig.getDefault();
        if(config.getField() != null){
            Collection<String> fields = StringUtils.isBlank(config.getField()) ? Collections.emptyList() : 
                Arrays.asList(StringUtils.split(config.getField(), ','));
            log.debug("set default similarity fields to {}", fields.isEmpty() ? "<default>" : fields);
            mltConfig.setSimilarityFields(null, fields);
        }
        //language specific configuration
        for(Entry<String,String> langEntry : config.getLang().entrySet()){
            if(langEntry.getValue() != null){
                Collection<String> fields = StringUtils.isBlank(langEntry.getValue()) ? Collections.emptyList() : 
                    Arrays.asList(StringUtils.split(langEntry.getValue(), ','));
                log.debug("set similarity fields for language '{}' to {}", langEntry.getKey(), fields.isEmpty() ? "<default>" : fields);
                mltConfig.setSimilarityFields(langEntry.getKey(), fields);
            }
        }
        
    }

    @Override
    protected SolrClient getClient() throws SolrServerException {
        return solrClient;
    }

    @Override
    protected MltConfig getMltConf() {
        return mltConfig;
    }

}
