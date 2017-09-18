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

    private final PropertyInjectionTransformer propertyInjectionTransformer;

    public StaticWebResourceConfiguration(@Autowired(required = false) PropertyInjectionTransformer propertyInjectionTransformer) {
        this.propertyInjectionTransformer = propertyInjectionTransformer;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/public/", "classpath:/static/")
                .setCacheControl(createCacheConfig(maxIndexCacheAge))
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        final ResourceChainRegistration registration = registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/public/", "classpath:/static/",
                        "classpath:/META-INF/resources/")
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
