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

import io.redlink.smarti.auth.AttributedUserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public class UserDetailsResponse {

    private final AttributedUserDetails user;

    public UserDetailsResponse(AttributedUserDetails user) {
        this.user = user;
    }

    public Map<String, String> getAttributes() {
        return user.getAttributes();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    public String getUsername() {
        return user.getUsername();
    }
}
