/*
 * Copyright (c) 2016 Redlink GmbH.
 */
package io.redlink.smarti.configuration;

import io.redlink.smarti.utils.PropertyInjectionTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.util.concurrent.TimeUnit;

/**
 *
 */
@Configuration
public class StaticWebResourceConfiguration extends WebMvcConfigurerAdapter {

    @Value("${ui.cache.maxAge:864000}")
    private long maxCacheAge = 60*60*24*10; // 10 days in seconds

    @Value("${ui.cache.indexMaxAge:3600}")
    private long maxIndexCacheAge = 60*60; // 1 hour in seconds

    @Autowired(required = false)
    private PropertyInjectionTransformer propertyInjectionTransformer = null;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/public/", "classpath:/static/")
                .setCacheControl(createCacheConfig(maxIndexCacheAge))
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        final ResourceChainRegistration registration = registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/public/", "classpath:/static/")
                .setCacheControl(createCacheConfig(maxCacheAge))
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        if (propertyInjectionTransformer != null) {
            registration.addTransformer(propertyInjectionTransformer);
        }
    }

    private CacheControl createCacheConfig(long maxAgeSeconds) {
        if (maxAgeSeconds < 0) {
            return CacheControl.noCache();
        } else {
            return CacheControl.maxAge(maxAgeSeconds, TimeUnit.SECONDS);
        }
    }

    @Component
    public static class ServletCustomizer implements EmbeddedServletContainerCustomizer {
        @Override
        public void customize(ConfigurableEmbeddedServletContainer container) {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);

            // Make sure fonts are served with the correct mime-type (IE!)
            mappings.add("woff", "application/font-woff");
            mappings.add("woff2","application/font-woff2");
            mappings.add("ttf","application/x-font-truetype");
            mappings.add("eot","application/vnd.ms-fontobject");
            mappings.add("otf","application/vnd.ms-opentype");

            container.setMimeMappings(mappings);
        }
    }
}
