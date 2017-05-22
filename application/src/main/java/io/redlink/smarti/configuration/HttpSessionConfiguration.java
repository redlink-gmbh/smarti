/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.JdkMongoSessionConverter;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

@Configuration
@EnableMongoHttpSession
public class HttpSessionConfiguration {

    @Bean
    public JdkMongoSessionConverter mongoSessionConverter() {
        return new JdkMongoSessionConverter();
    }

}
