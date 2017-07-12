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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A QueryPreparator that loads modules async.
 */
public abstract class LeanStartupQueryPreparator extends QueryPreparator implements HealthIndicator {
    private final CountDownLatch initComplete;
    private Exception initException = null;
    private final AtomicBoolean initExceptionRecorded = new AtomicBoolean(false);
    private long startupTime = -1;

    @Autowired(required = false)
    private Optional<ExecutorService> executorService = Optional.empty();

    public LeanStartupQueryPreparator(Phase phase) {
        this(phase, 0);
    }
    public LeanStartupQueryPreparator(Phase phase, int weight) {
        super(phase, weight);
        initComplete = new CountDownLatch(1);
    }

    @PostConstruct
    protected final void postConstruct() {
        final long bootTime = System.currentTimeMillis();
        executorService.orElse(Executors.newSingleThreadExecutor())
                .execute(() -> {
                    try {
                        init();
                    } catch (final Exception t) {
                        log.error("Error while initializing QueryPreparator: {}", t.getMessage());
                        initException = t;
                    } finally {
                        startupTime = System.currentTimeMillis() - bootTime;
                        initComplete.countDown();
                    }
                });
    }

    protected abstract void init() throws Exception;

    @Override
    public final void prepare(ProcessingData processingData) {
        try {
            initComplete.await();
            if (initException != null) {
                if (initExceptionRecorded.compareAndSet(false, true)) {
                    throw new IllegalStateException("Error during initialisation", initException);
                } else {
                    return;
                }
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted during initialisation", e);
        }

        doPrepare(processingData);
    }

    protected abstract void doPrepare(ProcessingData processingData);

    @Override
    public final Health health() {
        final Health.Builder builder = getHealth();
        if (initComplete.getCount() > 0) {
            // Still loading...
            builder.outOfService()
                    .withDetail("startup", "loading");
        } else if (initException == null) {
            // All OK, don't change the overall state
            builder.withDetail("startup", "complete")
                    .withDetail("startupDuration", startupTime);
        } else {
            // Error during startup
            builder.down(initException)
                    .withDetail("startup", "error")
                    .withDetail("startupDuration", startupTime);
        }

        return builder.build();
    }

    protected Health.Builder getHealth() {
        return Health.up();
    }
}
