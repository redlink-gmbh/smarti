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
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to configure Solr Cores to be used to configure {@link InterestingTermExtractor}s
 * <pre>
 * keyword.solrmlt[0].name=extractor1
 * keyword.solrmlt[0].url=http://www.example.org/solr/core1
 * keyword.solrmlt[0].field=text
 * keyword.solrmlt[1].name=extractor2
 * keyword.solrmlt[1].url=http://www.smarti.org/solr/smarti
 * </pre>
 * 
 * @author westei
 *
 */
public class SolrMltInterestingTermConfiguration {

    private List<SolrMltConfig> solrmlt = new ArrayList<>();
    
    public List<SolrMltConfig> getSolrmlt() {
        return solrmlt;
    }
    
    public void setSolrmlt(List<SolrMltConfig> solrmlt) {
        this.solrmlt = solrmlt;
    }
    
    public static class SolrMltConfig {

        private String name;
        private String url;
        private String field;
        
        private Map<String,String>  lang = new HashMap<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public Map<String, String> getLang() {
            return lang;
        }
        
        public void setLang(Map<String, String> lang) {
            this.lang = lang;
        }
        
        public boolean isValid(){
            if(StringUtils.isBlank(url) && StringUtils.isBlank(name)){
                return false;
            }
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "SolrMltConfig [name=" + name + ", url=" + url + ", field=" + field + ", lang=" + lang + "]";
        }
        
        
    }
    
}
