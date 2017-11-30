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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 */
@ConfigurationProperties(prefix = "security.config")
public class SecurityConfigurationProperties {

    public enum SecurityImpl {
        mongo,
    }

    /**
     * auth provider
     */
    private SecurityImpl implementation = SecurityImpl.mongo;

    /**
     * target url after logout
     */
    private URI logoutRedirect = null;

    private MongoProperties mongo = new MongoProperties();

    public SecurityImpl getImplementation() {
        return implementation;
    }

    public void setImplementation(SecurityImpl implementation) {
        this.implementation = implementation;
    }

    public URI getLogoutRedirect() {
        return logoutRedirect;
    }

    public void setLogoutRedirect(URI logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
    }

    public MongoProperties getMongo() {
        return mongo;
    }

    public void setMongo(MongoProperties mongo) {
        this.mongo = mongo;
    }

    public static class MongoProperties {

        /**
         * enable self-signup. Allows anyone to create a new account.
         */
        private boolean enableSignup = false;

        /**
         * enable password recovery. Allow resetting the user-password via mail-verification token.
         */
        private boolean enablePasswordRecovery = false;

        /**
         * hash-algorithm to store passwords
         */
        private String passwordHasher = "SHA-256";

        /**
         * default password for the admin-account created during boot.
         * If {@code null}, a random password will be generated and printed to the log.
         */
        private String adminPassword = null;

        public String getPasswordHasher() {
            return passwordHasher;
        }

        public void setPasswordHasher(String passwordHasher) {
            this.passwordHasher = passwordHasher;
        }

        public boolean isEnableSignup() {
            return enableSignup;
        }

        public MongoProperties setEnableSignup(boolean enableSignup) {
            this.enableSignup = enableSignup;
            return this;
        }

        public boolean isEnablePasswordRecovery() {
            return enablePasswordRecovery;
        }

        public MongoProperties setEnablePasswordRecovery(boolean enablePasswordRecovery) {
            this.enablePasswordRecovery = enablePasswordRecovery;
            return this;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }
    }
}
