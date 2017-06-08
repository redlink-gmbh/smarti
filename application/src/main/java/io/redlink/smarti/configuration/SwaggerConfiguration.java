/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.configuration;

import io.swagger.annotations.Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfo(
                        "Smarti",
                        "the smart in assistify",
                        "1.0",
                        null,
                        new Contact(
                                "Redlink GmbH",
                                "http://redlink.co",
                                "hello@redlink.co"
                        ),
                        "proprietary",
                        "http://dev.redlink.io/terms/",
                        Collections.emptyList()
                ))
                .select()
                    .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                    .paths(PathSelectors.any())
                .build();
    }

}
