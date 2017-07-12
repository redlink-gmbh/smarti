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
import io.redlink.smarti.processor.keyword.solrmltit.SolrMltInterestingTermConfiguration.SolrMltConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.validation.BindException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * @author Rupert Westenthaler
 *
 */
@Configuration
@Import(SolrMltInterestingTermRegistrar.class)
public class SolrMltInterestingTermRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private List<SolrMltConfig> solrmlt = new ArrayList<>();
    
    /**
     * we use a single HttpClient for all SolrMLT connections
     */
    private CloseableHttpClient httpClient;
    
    private ConfigurableEnvironment environment;
    
    public List<SolrMltConfig> getSolrmlt() {
        return solrmlt;
    }
    
    public void setSolrmlt(List<SolrMltConfig> solrmlt) {
        this.solrmlt = solrmlt;
    }
    
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment)environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        log.debug("in registerBeanDefinitions(..)");
        SolrMltInterestingTermConfiguration config = new SolrMltInterestingTermConfiguration();
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(config);
        factory.setTargetName("keyword");
        factory.setPropertySources(environment.getPropertySources());
        factory.setConversionService(environment.getConversionService());
        try {
            factory.bindPropertiesToTarget();
        }
        catch (BindException ex) {
            throw new FatalBeanException("Could not bind DataSourceSettings properties", ex);
        }
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 128);
        params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 32);
        params.set(HttpClientUtil.PROP_FOLLOW_REDIRECTS, false);
        httpClient = HttpClientUtil.createClient(params);
        config.getSolrmlt().stream().filter(SolrMltConfig::isValid).forEach(mltConfig -> {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(SolrMltInterestingTermExtractor.class);
            ConstructorArgumentValues constructorArguments = new ConstructorArgumentValues();
            beanDefinition.setConstructorArgumentValues(constructorArguments);
            constructorArguments.addIndexedArgumentValue(0, httpClient);
            constructorArguments.addIndexedArgumentValue(1, mltConfig);
            log.debug("register bean definition for {}", mltConfig);
            registry.registerBeanDefinition(mltConfig.getName(), beanDefinition);
        });
    }
    
    @PreDestroy
    protected void closeHttpClient(){
        log.debug("in closeHttpClient(..) - @PreDestroy");
        if(httpClient != null){
            try {
                httpClient.close();
            } catch (IOException e) { /*ignore*/ }
        }
    }
}
