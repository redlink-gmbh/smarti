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
package io.redlink.smarti.webservice;

import io.redlink.smarti.auth.AttributedUserDetails;
import io.redlink.smarti.auth.mongo.MongoUserDetailsService;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.services.AccountService;
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.webservice.pojo.UserDetailsResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "auth", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@ConditionalOnBean(MongoUserDetailsService.class)
public class UserWebservice {

    private final AccountService accountService;
    private final EmailValidator emailValidator = EmailValidator.getInstance();

    private final AuthenticationService authenticationService;

    @Autowired
    public UserWebservice(AccountService accountService, AuthenticationService authenticationService) {
        this.accountService = accountService;
        this.authenticationService = authenticationService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public AuthUser getUser(@AuthenticationPrincipal AttributedUserDetails user) {
        // Public access
        return AuthUser.wrap(user);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> signUp(@RequestBody  Map<String, String> data) {
        // Public access
        final String userName = data.get("username"),
                password = data.get("password"),
                mail = data.get("email");
        if ( StringUtils.isNoneBlank(userName, password, mail) && emailValidator.isValid(mail)) {
            return ResponseEntity.ok(new UserDetailsResponse(accountService.createAccount(userName, mail, password)));
        } else {
            return ResponseEntity.badRequest().build();
        }

    }

    @RequestMapping(path = "{username:[^/]+}/recover", method = RequestMethod.POST)
    public ResponseEntity<?> recoverPassword(
            @PathVariable("username") String username,
            @RequestBody(required = false) Map<String,String> data
    ) {
        // Public access
        final String recoveryToken = data.get("token"),
                newPassword = data.get("password");

        if (StringUtils.isNotBlank(recoveryToken)) {
            if (StringUtils.isNotBlank(newPassword)) {
                boolean success = accountService.completePasswordRecovery(username, newPassword, recoveryToken);
                if (success) {
                    return ResponseEntity.ok().build();
                }
            }
        } else {
            accountService.startPasswordRecovery(username);
            return ResponseEntity.accepted().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @RequestMapping(path = "{username}/password", method = RequestMethod.PUT)
    public ResponseEntity<?> setPassword(
            @AuthenticationPrincipal UserDetails authentication,
            @PathVariable("username") String username,
            @RequestBody Map<String,String> data
    ) {
        // Access only for ADMIN or @me
        if (authenticationService.hasUsername(authentication, username) && authenticationService.hasRole(authentication, AuthenticationService.ADMIN)) {
            throw new AccessDeniedException("No access for " + authentication);
        }
        final String newPassword = data.get("password");

        if (StringUtils.isNotBlank(newPassword)) {
            accountService.setPassword(username, newPassword);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.badRequest().build();
        }

    }

    @RequestMapping("check")
    public boolean checkUsernameExists(@RequestParam("username") String username) {
        // Public access
        return accountService.hasAccount(username);
    }

    private static class AuthUser {
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
}
