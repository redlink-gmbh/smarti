package io.redlink.smarti.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.redlink.nlp.api.ProcessingException;
import io.redlink.nlp.api.Processor;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.processing.ProcessingData;

@Service
public class PrepaireService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final List<Processor> processors;

    public PrepaireService(Optional<List<Processor>> processors) {
        log.debug("available processors: {}", processors);
        this.processors = processors.orElse(Collections.emptyList());

        Collections.sort(this.processors);
        //TODO: black/white list
        //TODO: required/optional
    }

    public void prepare(Conversation conversation) {
        log.debug("Preparing query for {}", conversation);
        if(conversation.getTokens().removeAll(null)){
            log.warn("Parsed Conversation {} contained a NULL Token", conversation);
        }
        ProcessingData pd = ProcessingData.create(conversation);
        final long start = System.currentTimeMillis();
        processors.forEach(p -> {
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
    }

    
}
