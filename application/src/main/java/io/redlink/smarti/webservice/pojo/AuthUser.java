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
import io.redlink.smarti.model.SmartiUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

@Deprecated
public class AuthUser {
    private String name, displayName, email;
    private Set<String> roles;

    private AuthUser(AttributedUserDetails userDetails) {
        name = userDetails.getUsername();
        roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> StringUtils.removeStart(r,"ROLE_"))
                .collect(Collectors.toSet());

        displayName = userDetails.getAttribute(SmartiUser.ATTR_DISPLAY_NAME);
        email = userDetails.getAttribute(SmartiUser.ATTR_EMAIL);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static AuthUser wrap(AttributedUserDetails userDetails) {
        return userDetails != null ? new AuthUser(userDetails) : null;
    }
}
