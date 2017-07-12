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

package io.redlink.smarti.processor.keyword.solrmltit;

import io.redlink.smarti.processor.keyword.intrestingterms.InterestingTermExtractor;
import io.redlink.smarti.processor.keyword.intrestingterms.MltConfig;
import io.redlink.smarti.processor.keyword.solrmltit.SolrMltInterestingTermConfiguration.SolrMltConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

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
