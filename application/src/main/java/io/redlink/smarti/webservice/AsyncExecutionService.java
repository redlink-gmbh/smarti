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
package io.redlink.smarti.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.redlink.smarti.properties.HttpCallbackProperties;
import io.redlink.smarti.properties.MavenCoordinatesProperties;
import io.redlink.smarti.webservice.pojo.CallbackPayload;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ConnectionBackoffStrategy;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@EnableConfigurationProperties({MavenCoordinatesProperties.class, HttpCallbackProperties.class})
public class AsyncExecutionService {

    private final ExecutorService processingExecutor;
    private final ObjectMapper objectMapper;
    private final HttpClientBuilder httpClientBuilder;

    private final Logger log = LoggerFactory.getLogger(AsyncExecutionService.class);

    public AsyncExecutionService(
            ObjectMapper objectMapper,
            Optional<ExecutorService> processingExecutor,
            MavenCoordinatesProperties mavenCoordinatesProperties,
            HttpCallbackProperties httpCallbackProperties
    ) {
        this.processingExecutor = processingExecutor
                .orElseGet(() -> Executors.newFixedThreadPool(2));
        this.objectMapper = objectMapper;


        this.httpClientBuilder = HttpClientBuilder.create()
                .setRetryHandler((exception, executionCount, context) -> executionCount < httpCallbackProperties.getRetryCount())
                .setConnectionBackoffStrategy(new ConnectionBackoffStrategy() {
                    @Override
                    public boolean shouldBackoff(Throwable t) {
                        return t instanceof IOException;
                    }

                    @Override
                    public boolean shouldBackoff(HttpResponse resp) {
                        return false;
                    }
                })
                .setUserAgent(String.format("Smarti/%s AsyncExecutionService", StringUtils.defaultString(mavenCoordinatesProperties.getVersion(), "0.0")));

        log.info("Starting up AsyncExecutionService/{}", mavenCoordinatesProperties.getVersion());

        if(StringUtils.isNotBlank(httpCallbackProperties.getProxy().getHost())) {
            httpClientBuilder.setProxy(new HttpHost(
                    httpCallbackProperties.getProxy().getHost(),
                    httpCallbackProperties.getProxy().getPort(),
                    httpCallbackProperties.getProxy().getScheme()
            ));
        }


    }

    public <T> ResponseEntity<T> execute(Supplier<T> task,
                                         HttpStatus status,
                                         URI callbackUri,
                                         Object entityId, URI entityUri) {
        return execute(task, callbackUri,
                (result) -> ResponseEntity
                        .status(status)
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", entityUri))
                        .body(result),
                entityId, entityUri);
    }


    public <T> ResponseEntity<T> execute(Supplier<T> task,
            URI callbackUri,
            Function<T, ResponseEntity<T>> syncResponseBuilder,
            Object entityId, URI entityUri) {
        //noinspection unchecked
        return execute(task, callbackUri, syncResponseBuilder,
                () -> (ResponseEntity<T>) ResponseEntity
                        .accepted()
                        .header(HttpHeaders.LINK, String.format(Locale.ROOT, "<%s>; rel=\"self\"", entityUri))
                        .body(ImmutableMap.of("id", entityId, "_uri", entityUri))
        );
    }

    public <T> ResponseEntity<T> execute(Supplier<T> task,
                                         URI callbackUri,
                                         Function<T, ResponseEntity<T>> syncResponseBuilder,
                                         Supplier<ResponseEntity<T>> acceptResponseBuilder) {
        if (Objects.isNull(callbackUri) && syncResponseBuilder != null) {
            return syncResponseBuilder.apply(task.get());
        } else {
            this.processingExecutor.submit(() -> {
                final CallbackPayload<?> payload = executeTask(task);
                if(callbackUri != null){
                    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                        final HttpPost post = new HttpPost(callbackUri);
                        final String data = objectMapper.writeValueAsString(payload);
                        post.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
    
                        log.debug("Sending callback: {} '{}' -d '{}'", post.getMethod(), post.getURI(), data);
                        httpClient.execute(post, response -> null);
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.error("Callback to Rocket.Chat <{}> failed: {}", callbackUri, e.getMessage(), e);
                        } else {
                            log.error("Callback to Rocket.Chat <{}> failed: {}", callbackUri, e.getMessage());
                        }
                    }
                } //no callback configured
            });
            return acceptResponseBuilder.get();
        }
    }

    private <T> CallbackPayload<?> executeTask(Supplier<T> task) {
        try {
            final T result = task.get();
            return new CallbackPayload<>(result);
        } catch (Throwable t) {
            return new CallbackPayload<>(CallbackPayload.Result.error, t);
        }
    }
}
