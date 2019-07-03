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

package io.redlink.smarti;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@SpringBootApplication
@ComponentScan(
        basePackageClasses = Application.class,
        basePackages = {"io.redlink.nlp", "io.redlink.utils.spring"})
@EnableMongoRepositories(
        basePackageClasses = Application.class,
        basePackages = {"io.redlink.utils.spring"})
@EnableMongoAuditing //needed for @CreatedDate and @LastModifiedDate
@EnableAutoConfiguration(exclude = SolrAutoConfiguration.class)
@EnableConfigurationProperties
public class Application {

    private static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        final SpringApplication app = new SpringApplication(Application.class);
        // save the pid into a file...
        app.addListeners(new ApplicationPidFileWriter("smarti.pid"));

        final ConfigurableApplicationContext context = app.run(args);
        final ConfigurableEnvironment env = context.getEnvironment();

        try {
            //http://localhost:8080/admin/index.html
            final URI uri = new URI(
                    (env.getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http"),
                    null,
                    (env.getProperty("server.address", "localhost")),
                    (env.getProperty("server.port", Integer.class, 8080)),
                    (env.getProperty("server.context-path", "/")).replaceAll("//+", "/"),
                    null, null);

            log.info("{} started: {}",
                    env.getProperty("server.display-name", context.getDisplayName()),
                    uri);
        } catch (URISyntaxException e) {
            log.warn("Could not build launch-url: {}", e.getMessage());
        }
    }
}