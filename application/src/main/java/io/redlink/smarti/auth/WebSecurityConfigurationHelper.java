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
package io.redlink.smarti.auth;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Service;

/**
 * Spring Web Security Configuration
 *
 * @author Jakob Frank
 * @author Sergio Fernandez
 */
@Service
public class WebSecurityConfigurationHelper {

    private static final String[] STATIC_RESOURCES = {
            "/index.html",
            "/favicon.ico",
            "/robots.txt",
            "/fonts/**",
            "/scripts/**",
            "/styles/**"
    };

    @Value("${springfox.documentation.swagger.v2.path:/v2/api-docs}")
    private String swaggerPath = "/v2/api-docs";

    public void preConfigure(HttpSecurity http) throws Exception {}

    public void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .cors()
                .and()
            .csrf()
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .disable()
            .anonymous()
                .and()
            .authorizeRequests()
                // allow everyone to access swagger
                .antMatchers(swaggerPath)
                    .permitAll()
                // everyone to access the static resources
                .antMatchers(STATIC_RESOURCES)
                    .permitAll()
                // allow access to the auth-services
                .antMatchers("/auth", "/auth/**")
                    .permitAll()
                // roles/permissions are checked in Webservice implementations
                .anyRequest()
                    .authenticated()
                .and()
        ;
         // @formatter:on

        LoggerFactory.getLogger(WebSecurityConfigurationHelper.class).info(http.toString());
    }
}
