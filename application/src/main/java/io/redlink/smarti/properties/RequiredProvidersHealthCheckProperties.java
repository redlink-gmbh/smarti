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
package io.redlink.smarti.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "smarti.required-providers")
public class RequiredProvidersHealthCheckProperties {

    /**
     * let healthcheck fail (DOWN) if a required provider is missing
     */
    private boolean failOnMissing = true;

    /**
     * let healthcheck fail (DONW) if a specific provider is missing
     */
    private Map<String, Boolean> failIfMissing = new HashMap<>();

    public boolean isFailOnMissing() {
        return failOnMissing;
    }

    public void setFailOnMissing(boolean failOnMissing) {
        this.failOnMissing = failOnMissing;
    }

    public Map<String, Boolean> getFailIfMissing() {
        return failIfMissing;
    }

    public void setFailIfMissing(Map<String, Boolean> failIfMissing) {
        this.failIfMissing = failIfMissing;
    }

}
