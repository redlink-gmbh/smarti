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

package io.redlink.smarti.services;

import io.redlink.nlp.api.ProcessingData;
import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.processing.AnalysisConfiguration;
import io.redlink.smarti.processing.AnalysisData;
import io.redlink.smarti.processing.AnalysisLanguageConfiguration;
import io.redlink.smarti.processing.MessageContentProcessor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
@EnableConfigurationProperties(AnalysisConfiguration.class)
public class PrepareService {


    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String ANALYSIS_CONFIGURATION_CATEGORY = "Analysis";

    private static Set<String> REQUIRED = Collections.unmodifiableSet(Collections.emptySet());
    private static Set<String> OPTIONAL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("*")));

//    @Value("${smarti.analysis.required:}")
//    private String requiredProcessors;
//    
//    @Value("${smarti.analysis.optional:}")
//    private String optionalProcessors;

    private AnalysisConfiguration analysisConfig;
    private ConfigurationService confService;
    private AnalysisLanguageConfiguration analysisLanguageConfig;
    /*
     * NOTE: Only used for initialization. Do not access 
     */
    private final List<Processor> _processors;
    
    private final List<Processor> pipeline = new ArrayList<>();
    
    private final MessageContentProcessor messageContentProvider;
    

    public PrepareService(AnalysisConfiguration analysisConfig, 
            AnalysisLanguageConfiguration analysisLanguageConfig,
            Optional<ConfigurationService> configService, 
            Optional<List<Processor>> processors,
            Optional<MessageContentProcessor> messageContentProvider) {
        this.analysisConfig = analysisConfig;
        this.confService = configService.orElse(null);
        this.analysisLanguageConfig = analysisLanguageConfig;
        this.messageContentProvider = messageContentProvider.orElse(null);
        log.debug("available processors: {}", processors);
        this._processors = processors.orElse(Collections.emptyList());

        Collections.sort(this._processors);
        //TODO: black/white list
        //TODO: required/optional
    }

    @PostConstruct
    protected void initPipeline(){
        log.info("> configure Smarti analysis pipeline");
        Set<String> required;
        Set<String> optional;
        Set<String> blacklist = new HashSet<>();
        if(StringUtils.isNotBlank(analysisConfig.getPipeline().getRequired())){
            log.info("use configured required Processors: [{}]", analysisConfig.getPipeline().getRequired());
            required = new HashSet<>();
            for(String proc : StringUtils.split(analysisConfig.getPipeline().getRequired(), ',')){
                proc = StringUtils.trimToNull(proc);
                if(proc != null){
                    required.add(proc);
                } //else ignore
            }
        } else {
            log.info("use default required Processors: {}", REQUIRED);
            required = new HashSet<>(REQUIRED);
        }
        if(StringUtils.isNotBlank(analysisConfig.getPipeline().getOptional())){
            log.info("use configured optional Processors: [{}]", analysisConfig.getPipeline().getOptional());
            optional = new HashSet<>();
            for(String proc : StringUtils.split(analysisConfig.getPipeline().getOptional(), ',')){
                proc = StringUtils.trimToNull(proc);
                if(proc != null){
                    if(proc.charAt(0) == '!'){
                        blacklist.add(proc.substring(1));
                    } else {
                        optional.add(proc);
                    }
                } //else ignore
            }
        } else {
            log.info("use default optional Processors: {}", REQUIRED);
            optional = new HashSet<>(OPTIONAL);
        }
        boolean wildcard = optional.contains("*");
        log.debug("{} processors present", _processors.size());
        for(Processor p : _processors){
            if(required.remove(p.getKey())){
                pipeline.add(p);
                log.debug("  + {} (required)", p);
            } else if(!blacklist.contains(p.getKey()) && (wildcard || optional.contains(p.getKey()))){
                pipeline.add(p);
                log.debug("  + {}", p);
            } else {
                log.debug("  - {}", p);
            }
            optional.remove(p.getKey());
        }
        if(!required.isEmpty()){
            throw new IllegalStateException("Missing required Processors " + required);
        }
        Collections.sort(pipeline);
        log.info("analysis pipeline: {}", pipeline);
        if(!optional.isEmpty() && log.isInfoEnabled()){
            log.info(" - {} optional processors are not available {}", optional.size(), optional);
        }
        //we do no longer need to hold references to all processors as we do now have a configured pipeline
        _processors.clear();
    }
    
    public Analysis prepare(Client client, Conversation conversation, Date date) {
        Analysis analysis = new Analysis(client.getId(), conversation.getId(), date);
        //TODO: get pipeline and processor configuration for the parsed client
        log.debug("Preparing query for {}", conversation);
        AnalysisData pd = AnalysisData.create(conversation, analysis, messageContentProvider, analysisConfig.getConextSize());
        
        //The configuration allows to define the language of the conversation
        String conversationLanguage = null;
        Configuration config = confService != null ? confService.getClientConfiguration(client) : null;
        if(config != null){
            Optional<String> clientLanguage = StreamSupport.stream(config.getConfigurations(analysisLanguageConfig).spliterator(),false)
                .filter(ComponentConfiguration::isEnabled)
                .map(cc -> cc.getConfiguration(AnalysisLanguageConfiguration.KEY_LANGUAGE, ""))
                .filter(StringUtils::isNotEmpty)
                .findFirst();
            if(clientLanguage.isPresent()){
                log.debug(" set conversation language to '{}' (client configuration)", clientLanguage.get());
                conversationLanguage = clientLanguage.get();
            }
        }
        if(conversationLanguage == null && StringUtils.isNotBlank(analysisConfig.getLanguage())){
            log.debug(" set conversation language to '{}' (global configuration)", analysisConfig.getLanguage());
            conversationLanguage = analysisConfig.getLanguage();
        }
        
        if(conversationLanguage != null){
            pd.getConfiguration().put(ProcessingData.Configuration.LANGUAGE, conversationLanguage);            
        }
        
        final long start = System.currentTimeMillis();
        pipeline.forEach(p -> {
            log.debug(" -> calling {}", p.getClass().getSimpleName());
            try {
                p.process(pd);
                log.trace("  <- completed {}", p.getClass().getSimpleName());
            } catch (ProcessingException e) {
                log.warn("Unable to process {} with Processor {} (class: {}) ", conversation, p, p.getClass().getName());
                log.debug("STACKTRACE", e);
                //TODO: check if this was a required or an optional processor
            }
        });
        log.debug("analysed Conversation[id:{}] in {}ms", conversation.getId(), start-System.currentTimeMillis());
        //now sort the Tokens
        Collections.sort(analysis.getTokens());
        return analysis;
    }
}
