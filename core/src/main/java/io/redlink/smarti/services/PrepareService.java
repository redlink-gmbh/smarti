package io.redlink.smarti.services;

import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.processing.ProcessingData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class PrepareService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static Set<String> REQUIRED = Collections.unmodifiableSet(Collections.emptySet());
    private static Set<String> OPTIONAL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("*")));

    @Value("${smarti.analysis.required:}")
    private String requiredProcessors;
    
    @Value("${smarti.analysis.optional:}")
    private String optionalProcessors;

    /*
     * NOTE: Only used for initialization. Do not access 
     */
    private final List<Processor> _processors;
    
    private final List<Processor> pipeline = new ArrayList<>();
    

    public PrepareService(Optional<List<Processor>> processors) {
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
        if(StringUtils.isNotBlank(requiredProcessors)){
            log.info("use configured required Processors: [{}]", requiredProcessors);
            required = new HashSet<>();
            for(String proc : StringUtils.split(requiredProcessors, ',')){
                proc = StringUtils.trimToNull(proc);
                if(proc != null){
                    required.add(proc);
                } //else ignore
            }
        } else {
            log.info("use default required Processors: {}", REQUIRED);
            required = new HashSet<>(REQUIRED);
        }
        if(StringUtils.isNotBlank(optionalProcessors)){
            log.info("use configured optional Processors: [{}]", optionalProcessors);
            optional = new HashSet<>();
            for(String proc : StringUtils.split(optionalProcessors, ',')){
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
    
    public void prepare(Conversation conversation) {
        log.debug("Preparing query for {}", conversation);
        while(conversation.getTokens().remove(null)){
            log.warn("Parsed Conversation {} contained a NULL Token", conversation);
        }
        ProcessingData pd = ProcessingData.create(conversation);
        final long start = System.currentTimeMillis();
        pipeline.forEach(p -> {
            log.debug(" -> calling {}", p.getClass().getSimpleName());
            try {
                p.process(pd);
                log.trace("  <- completed {}", p.getClass().getSimpleName());
            } catch (ProcessingException e) {
                log.warn("Unable to process {} with Processor {} (class: {}) ", conversation, p, p.getClass().getName());
                //TODO: check if this was a required or an optional processor
            }
        });
        log.debug("prepared Conversation[id:{}] in {}ms", conversation.getId(), start-System.currentTimeMillis());
        conversation.getMeta().setLastMessageAnalyzed(conversation.getMessages().size()-1);
        log.trace("set lastMessageAnalyzed: {}", conversation.getMeta().getLastMessageAnalyzed());
    }

    
}
