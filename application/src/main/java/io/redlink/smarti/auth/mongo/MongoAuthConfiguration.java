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
package io.redlink.smarti.auth.mongo;

import io.redlink.smarti.auth.SecurityConfigurationProperties;
import io.redlink.smarti.auth.WebSecurityConfigurationHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;

/**
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "security.config", name = "implementation", havingValue = "mongo")
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
public class MongoAuthConfiguration extends WebSecurityConfigurerAdapter {
    private Logger log = LoggerFactory.getLogger(MongoAuthConfiguration.class);

    private final SecurityConfigurationProperties securityConfig;
    private final MongoUserDetailsService mongoUserDetailsService;
    private final WebSecurityConfigurationHelper webSecurityConfigurationHelper;

    public MongoAuthConfiguration(SecurityConfigurationProperties securityConfig,
                                  MongoUserDetailsService mongoUserDetailsService,
                                  WebSecurityConfigurationHelper webSecurityConfigurationHelper) {
        this.securityConfig = securityConfig;
        this.mongoUserDetailsService = mongoUserDetailsService;
        this.webSecurityConfigurationHelper = webSecurityConfigurationHelper;
    }


    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        log.info("Configuring Spring-Security with MongoDB based user-management");
        final DaoAuthenticationConfigurer<AuthenticationManagerBuilder, MongoUserDetailsService> authenticationConfigurer = auth.userDetailsService(mongoUserDetailsService);
        if (StringUtils.isNotBlank(securityConfig.getMongo().getPasswordHasher())) {
            authenticationConfigurer.passwordEncoder(new MessageDigestPasswordEncoder(securityConfig.getMongo().getPasswordHasher()));
        } else {
            log.warn("no password-hasher configured - are you sure you want to store your users' passwords in plain-text?");
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Set the global defaults
        webSecurityConfigurationHelper.preConfigure(http);

        // and enable Form-Login and basic auth
        http.formLogin()
                .permitAll();
        http.httpBasic();
        // and Logout
        final LogoutConfigurer<HttpSecurity> logout = http.logout();
        if (securityConfig.getLogoutRedirect() != null) {
            logout.logoutSuccessUrl(securityConfig.getLogoutRedirect().toASCIIString());
        }

        // append the default security settings
        webSecurityConfigurationHelper.configure(http);
    }
}
