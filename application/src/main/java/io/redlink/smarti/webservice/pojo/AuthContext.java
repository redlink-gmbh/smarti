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
package io.redlink.smarti.webservice.pojo;

import org.springframework.security.core.Authentication;

public class AuthContext {

    private final String authToken;
    private final Authentication authentication;

    public AuthContext(String authToken, Authentication authentication) {
        this.authToken = authToken;
        this.authentication = authentication;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
                .append(getClass().getSimpleName())
                .append("(token=")
                .append(authToken)
                .append(",login=");

        if (authentication != null) {
            builder.append(authentication.getName());
        } else {
            builder.append("null");
        }
        return builder.append(")").toString();
    }
}
