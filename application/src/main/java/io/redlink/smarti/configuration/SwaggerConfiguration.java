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

import io.redlink.smarti.webservice.pojo.AuthContext;
import io.swagger.annotations.Api;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Value("${swagger.api.version:1.0}")
    private String apiVersion = "1.0";

    @Value("${project.organization.name:Redlink GmbH}")
    private String apiContactName = "Redlink GmbH";
    @Value("${project.organization.url:http://redlink.co}")
    private String apiContactUrl = "http://redlink.co";



    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfo(
                        "Smarti",
                        "the smart in assistify",
                        apiVersion,
                        null,
                        new Contact(
                                apiContactName,
                                apiContactUrl,
                                "hello@redlink.co"
                        ),
                        "Apache 2.0",
                        "https://www.apache.org/licenses/LICENSE-2.0",
                        Collections.emptyList()
                ))
                .select()
                    .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                    .paths(PathSelectors.any())
                    .build()
                .securitySchemes(Arrays.asList(authToken(), basicAuth()))
                .securityContexts(Arrays.asList(publicContext(), defaultContext()))
                .ignoredParameterTypes(AuthContext.class)
                .directModelSubstitute(ObjectId.class, String.class)
                .directModelSubstitute(URL.class, String.class)
                .directModelSubstitute(URI.class, String.class);
    }

    private SecurityContext publicContext() {
        return SecurityContext.builder()
                .forPaths(PathSelectors.regex("/auth(/.*)?"))
                .build();
    }

    private SecurityContext defaultContext() {
        return SecurityContext.builder()
                .forPaths(PathSelectors.any())
                .securityReferences(defaultAuth())
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        final AuthorizationScope[] authScopes = {};
        return Arrays.asList(
                new SecurityReference(basicAuth().getName(), authScopes),
                new SecurityReference(authToken().getName(), authScopes)
        );
    }

    private BasicAuth basicAuth() {
        return new BasicAuth("basic");
    }

    private ApiKey authToken() {
        return new ApiKey("token", "X-Auth-Token", "header");
    }

}
