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
