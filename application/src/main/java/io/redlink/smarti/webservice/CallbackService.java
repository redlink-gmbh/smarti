package io.redlink.smarti.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.redlink.smarti.properties.HttpCallbackProperties;
import io.redlink.smarti.properties.MavenCoordinatesProperties;
import io.redlink.smarti.webservice.pojo.CallbackPayload;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ConnectionBackoffStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;

@Service
@EnableConfigurationProperties(value={MavenCoordinatesProperties.class,HttpCallbackProperties.class})
public class CallbackService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ObjectMapper objectMapper;
    private HttpClientBuilder httpClientBuilder;

    public CallbackService(
            ObjectMapper objectMapper,
            MavenCoordinatesProperties mavenCoordinatesProperties,
            HttpCallbackProperties httpCallbackProperties
    ) {
        this.objectMapper = objectMapper;


        this.httpClientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build()
                )
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
    
    public Void execute(URI callbackUri, CallbackPayload<?> payload){
        if(callbackUri == null || payload == null){
            return null;
        }
        HttpPost request = new HttpPost(callbackUri);
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            final String data = objectMapper.writeValueAsString(payload);
            request.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

            log.debug("Sending callback: {} '{}' -d '{}'", request.getMethod(), request.getURI(), data);
            StatusLine status = httpClient.execute(request, response -> response.getStatusLine());
            log.debug("'{}' for callback: {} '{}'", status, request);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Callback to <{}> failed: {}", request, e.getMessage(), e);
            } else {
                log.error("Callback to <{}> failed: {}", request, e.getMessage());
            }
        }
        return null;
    }
    
}
