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
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDetailsResponse {

    private final String login;
    private final Set<String> roles;

    private UserDetailsResponse(AttributedUserDetails userDetails) {
        login = userDetails.getUsername();
        roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> StringUtils.removeStart(r,"ROLE_"))
                .collect(Collectors.toSet());
    }

    public String getLogin() {
        return login;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public static UserDetailsResponse wrap(AttributedUserDetails attributedUserDetails) {
        return Objects.nonNull(attributedUserDetails)?new UserDetailsResponse(attributedUserDetails):null;
    }
}
