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
package io.redlink.smarti.health;

import io.redlink.nlp.api.Processor;
import io.redlink.nlp.stanfordnlp.StanfordNlpProcessor;
import io.redlink.nlp.truecase.de.GermanTrueCaseExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Component
public class RequiredProvidersHealthCheck extends AbstractHealthIndicator {

    private final Logger log = LoggerFactory.getLogger(RequiredProvidersHealthCheck.class);

    private final StanfordNlpProcessor stanfordNlpProcessor;

    private final GermanTrueCaseExtractor germanTrueCaseExtractor;

    public RequiredProvidersHealthCheck(Optional<StanfordNlpProcessor> stanfordNlpProcessor,
                                        Optional<GermanTrueCaseExtractor> germanTrueCaseExtractor) {
        this.stanfordNlpProcessor = stanfordNlpProcessor.orElse(null);
        this.germanTrueCaseExtractor = germanTrueCaseExtractor.orElse(null);
    }

    @PostConstruct
    protected void onStartup() {
        if (stanfordNlpProcessor == null) {
            log.error("Missing Processor: 'stanfordNlpProcessor'");
        } else {
            log.info("Found Processor: 'stanfordNlpProcessor'");
        }
        if (germanTrueCaseExtractor == null) {
            log.error("Missing Processor: 'germanTrueCaseExtractor'");
        } else {
            log.info("Found Processor: 'germanTrueCaseExtractor'");
        }
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.up();
        checkStanfordNlpProcessor(builder);
        checkGermanTrueCaseExtractor(builder);
    }

    private void checkGermanTrueCaseExtractor(Health.Builder builder) {
        appendProviderDetails(builder, "germanTrueCaseExtractor", germanTrueCaseExtractor);
    }

    private void checkStanfordNlpProcessor(Health.Builder builder) {
        appendProviderDetails(builder, "stanfordNlpProcessor", stanfordNlpProcessor);
    }

    private void appendProviderDetails(Health.Builder builder, String name, Processor processor) {
        final Health.Builder sub = new Health.Builder(Status.UP);
        if (processor != null) {
            sub
                    .withDetail("key", processor.getKey())
                    .withDetail("name", processor.getName())
                    .withDetail("class", processor.getClass().getName())
                    ;
        } else {
            sub.outOfService();
            builder.down();
        }
        builder.withDetail(name, sub);
    }
}
