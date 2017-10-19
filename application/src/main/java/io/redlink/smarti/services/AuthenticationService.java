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
package io.redlink.smarti.services;

import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
public class AuthenticationService {

    public static final String ADMIN = "ADMIN";

    private final UserService userService;

    @Autowired
    public AuthenticationService(UserService userService) {
        this.userService = userService;
    }

    public boolean hasUsername(Authentication authentication, String username) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getName(), username);
    }

    public boolean hasUsername(UserDetails authentication, String username) {
        return Objects.nonNull(authentication) && Objects.equals(authentication.getUsername(), username);
    }

    public boolean hasRole(Authentication authentication, String role) {
        return hasAnyRole(authentication, role);
    }

    public boolean hasRole(UserDetails authentication, String role) {
        return hasAnyRole(authentication, role);
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        return hasAnyRole(authentication, ImmutableSet.copyOf(roles));
    }

    public boolean hasAnyRole(UserDetails authentication, String... roles) {
        return hasAnyRole(authentication, ImmutableSet.copyOf(roles));
    }

    public boolean hasAnyRole(Authentication authentication, Set<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roles::contains);
    }

    public boolean hasAnyRole(UserDetails authentication, Set<String> roles) {
        return Objects.nonNull(authentication) &&
                authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roles::contains);
    }

    public Set<ObjectId> getClients(Authentication authentication) {
        return userService.getClientsForUser(authentication.getName());
    }

    public Set<ObjectId> getClients(UserDetails authentication) {
        return userService.getClientsForUser(authentication.getUsername());
    }
}
