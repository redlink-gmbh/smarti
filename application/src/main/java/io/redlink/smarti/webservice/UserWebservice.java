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
import io.redlink.smarti.auth.SecurityConfigurationProperties;
import io.redlink.smarti.auth.mongo.MongoUserDetailsService;
import io.redlink.smarti.services.AccountService;
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.UserService;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.redlink.smarti.webservice.pojo.SmartiUserData;
import io.redlink.smarti.webservice.pojo.UserDetailsResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@ConditionalOnBean(MongoUserDetailsService.class)
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
@Api
public class UserWebservice {

    private final EmailValidator emailValidator = EmailValidator.getInstance();

    private final AccountService accountService;
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    public UserWebservice(AccountService accountService, AuthenticationService authenticationService, UserService userService, SecurityConfigurationProperties securityConfigurationProperties) {
        this.accountService = accountService;
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @ApiOperation("retrieve current user details")
    @RequestMapping(value = "/auth", method = RequestMethod.GET)
    public UserDetailsResponse getUser(@AuthenticationPrincipal AttributedUserDetails user) {
        // Public access
        return UserDetailsResponse.wrap(user);
    }

    @ApiOperation(value = "signup", notes = "create a new account", response = UserDetailsResponse.class)
    @RequestMapping(value = "/auth/signup", method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> signUp(@RequestBody  Map<String, String> data) {
        if (!securityConfigurationProperties.getMongo().isEnableSignup()) {
            return ResponseEntity.badRequest().build();
        }
        // Public access
        final String login = data.get("login"),
                password = data.get("password"),
                mail = data.get("email");
        if ( StringUtils.isNoneBlank(login, password, mail) && emailValidator.isValid(mail)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(UserDetailsResponse.wrap(accountService.createAccount(login, mail, password)));
        } else {
            return ResponseEntity.badRequest().build();
        }

    }

    @ApiOperation(value = "password recover", notes = "recover password: either start or complete the password recovery process")
    @RequestMapping(path = "/auth/recover", method = RequestMethod.POST)
    public ResponseEntity<?> recoverPassword(
            @RequestParam("user") String login,
            @RequestBody(required = false) Map<String,String> data
    ) {
        if (!securityConfigurationProperties.getMongo().isEnablePasswordRecovery()) {
            return ResponseEntity.badRequest().build();
        }

        // Public access
        if (data == null) data = new HashMap<>();
        final String recoveryToken = data.get("token"),
                newPassword = data.get("password");

        if (StringUtils.isNotBlank(recoveryToken)) {
            if (StringUtils.isNotBlank(newPassword)) {
                boolean success = accountService.completePasswordRecovery(login, newPassword, recoveryToken);
                if (success) {
                    return ResponseEntity.ok().build();
                }
            }
        } else {
            if (accountService.startPasswordRecovery(login)) {
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        return ResponseEntity.badRequest().build();
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public List<SmartiUserData> listUsers(AuthContext authContext, @RequestParam(name = "filter", required = false) String filter) {
        authenticationService.assertRole(authContext, AuthenticationService.ADMIN);

        return userService.listUsers(filter).stream()
                .map(SmartiUserData::fromModel)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<SmartiUserData> createUser(AuthContext authContext,
                                 @RequestBody SmartiUserData user) {
        authenticationService.assertRole(authContext, AuthenticationService.ADMIN);

        if (StringUtils.isBlank(user.getLogin())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (userService.existsUsername(user.getLogin())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(SmartiUserData.fromModel(userService.createUser(user.toModel())));
    }

    @RequestMapping(value = "/user/{login}", method = RequestMethod.GET)
    public SmartiUserData getUser(AuthContext authentication,
                              @PathVariable("login") String login) {
        // Access only for ADMIN or @me
        if (authenticationService.hasLogin(authentication, login)
                || authenticationService.hasRole(authentication, AuthenticationService.ADMIN)) {
            return SmartiUserData.fromModel(userService.getUser(login));
        } else {
            throw new AccessDeniedException("No access for " + authentication);
        }
    }

    @RequestMapping(value = "/user/{login}", method = RequestMethod.PUT, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public SmartiUserData updateUser(AuthContext authentication,
                                 @PathVariable("login") String login,
                                 @RequestBody SmartiUserData user) {
        // Access only for ADMIN or @me
        if (authenticationService.hasLogin(authentication, login)
                || authenticationService.hasRole(authentication, AuthenticationService.ADMIN)) {

            return SmartiUserData.fromModel(userService.updateProfile(login, user.getProfile()));
        } else {
            throw new AccessDeniedException("No access for " + authentication);
        }
    }

    @RequestMapping(value = "/user/{login}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteUser(AuthContext authContext,
                                        @PathVariable("login") String login) {
        authenticationService.assertRole(authContext, AuthenticationService.ADMIN);

        if (accountService.deleteUser(login)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation("update password")
    @RequestMapping(path = "/user/{login}/password", method = RequestMethod.PUT)
    public ResponseEntity<?> setPassword(
            AuthContext authentication,
            @PathVariable("login") String login,
            @RequestBody Map<String,String> data
    ) {
        // Access only for ADMIN or @me
        if (authenticationService.hasLogin(authentication, login)
                || authenticationService.hasRole(authentication, AuthenticationService.ADMIN)) {
            final String newPassword = data.get("password");

            if (StringUtils.isNotBlank(newPassword)) {
                accountService.setPassword(login, newPassword);
                return ResponseEntity.ok(userService.getUser(login));
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            throw new AccessDeniedException("No access for " + authentication);
        }
    }

    @RequestMapping(path = "/user/{login}/roles", method = RequestMethod.PUT)
    public ResponseEntity<?> setRoles(
            AuthContext authentication,
            @PathVariable("login") String login,
            @RequestBody Set<String> roles
    ) {
        authenticationService.assertRole(authentication, AuthenticationService.ADMIN);

        if (accountService.setRoles(login, roles)) {
            return ResponseEntity.ok(userService.getUser(login));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @ApiOperation(value = "check login", notes = "check if the provided login is already taken")
    @RequestMapping(value = "/auth/check", method = RequestMethod.GET)
    public boolean checkLoginExists(@RequestParam("login") String login) {
        // Public access
        return accountService.hasAccount(login);
    }

}
