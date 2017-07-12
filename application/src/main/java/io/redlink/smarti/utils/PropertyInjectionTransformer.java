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
package io.redlink.smarti.utils;

import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link ResourceTransformer} that replaces {@code @{property:default}@} with the values from the
 * {@link Environment}.
 */
@Component
public class PropertyInjectionTransformer implements ResourceTransformer {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Pattern TEXT_REPLACE_PATTERN = Pattern.compile("@\\{([\\w.-]+)(?::([^}]*))?\\}@");

    private final AntPathMatcher pathMatcher;

    @Value("${resource.transform.inject.text:constants*.js}")
    private Set<String> injectableTextFiles = Sets.newHashSet("constants*.js");

    @Autowired
    private Environment environment;

    private Logger log = LoggerFactory.getLogger(getClass());

    public PropertyInjectionTransformer() {
        pathMatcher = new AntPathMatcher();
        pathMatcher.setCachePatterns(true);
        pathMatcher.setCaseSensitive(false);
    }

    public PropertyInjectionTransformer(Set<String> injectableTextFiles) {
        this();
        this.injectableTextFiles = injectableTextFiles;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
        resource = transformerChain.transform(request, resource);

        final String fileName = resource.getFilename();
        final String fileExt = FilenameUtils.getExtension(fileName);
        switch (fileExt) {
            case "js":
            case "json":
            case "css":
            case "html":
                log.trace("format {} supported for Text-Injection", fileExt);
                return transformText(fileName, resource);
            default:
                log.trace("unsupported Resource-Type: {}", fileExt);
                return resource;
        }
    }

    protected Resource transformText(String fileName, Resource resource) throws IOException {
        if (injectableTextFiles.stream()
                .noneMatch(p -> pathMatcher.match(p, fileName))) {
            log.trace("{} not in injectable-files, skipping", fileName);
            return resource;
        } else {
            log.trace("transforming {}", fileName);
        }

        final byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        final String content = new String(bytes, DEFAULT_CHARSET);
        final StringBuffer transformed = new StringBuffer();

        final Matcher m = TEXT_REPLACE_PATTERN.matcher(content);
        while (m.find()) {
            final String match = m.group(),
                    key = m.group(1),
                    fallback = StringUtils.defaultString(m.group(2), match);
            if (log.isTraceEnabled()) {
                final String replace = environment.getProperty(key);
                if (replace == null) {
                    if (m.group(2) == null) {
                        log.trace("{} -> '{}' (property not found, no fallback)", match, fallback);
                    } else {
                        log.trace("{} -> '{}' (fallback)", match, fallback);
                    }
                } else {
                    log.trace("{} -> '{}' (env '{}')", match, replace, key);
                }
            }
            m.appendReplacement(transformed, Matcher.quoteReplacement(environment.getProperty(key, fallback)));
        }
        m.appendTail(transformed);

        return new TransformedResource(resource, transformed.toString().getBytes(DEFAULT_CHARSET));
    }

}
