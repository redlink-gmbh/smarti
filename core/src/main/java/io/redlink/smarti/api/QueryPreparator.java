/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.api;

import io.redlink.smarti.processing.ProcessingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public abstract class QueryPreparator implements Comparable<QueryPreparator> {

    public enum Phase {
        pre(-500),
        nlp(-100),
        langDetect(-90),
        pos(-50),
        lemma(-40),
        stem(-40),
        stopword(-40),
        negation(-30),
        ner(-20),
        extraction(0),
        post(100)
        ;
        

        private int weight;

        Phase(int w) {
            weight = w;
        }
    }

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final int weight;

    private QueryPreparator(int weight) {
        this.weight = weight;
    }

    protected QueryPreparator(Phase phase) {
        this(phase.weight);
    }

    protected QueryPreparator(Phase pahse, int weight) {
        this(pahse.weight + weight);
    }


    public abstract void prepare(ProcessingData processingData);


    @Override
    public int compareTo(QueryPreparator other) {
        return Integer.compare(this.weight, other.weight);
    }
}
