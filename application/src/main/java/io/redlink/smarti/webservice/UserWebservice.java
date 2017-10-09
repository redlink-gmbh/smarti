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

import io.redlink.smarti.auth.mongo.MongoUserDetailsService;
import io.redlink.smarti.services.AccountService;
import io.redlink.smarti.webservice.pojo.UserDetailsResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping(value = "auth", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@ConditionalOnBean(MongoUserDetailsService.class)
public class UserWebservice {

    private final AccountService accountService;
    private final EmailValidator emailValidator = EmailValidator.getInstance();

    @Autowired
    public UserWebservice(AccountService accountService) {
        this.accountService = accountService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Principal getUser(Principal user) {
        return (user);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDetailsResponse> signUp(@RequestBody  Map<String, String> data) {
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
            @PathVariable("username") String userId,
            @RequestBody(required = false) Map<String,String> data
    ) {
        final String recoveryToken = data.get("token"),
                newPassword = data.get("password");

        if (StringUtils.isNotBlank(recoveryToken)) {
            if (StringUtils.isNotBlank(newPassword)) {
                boolean success = accountService.completePasswordRecovery(userId, newPassword, recoveryToken);
                if (success) {
                    return ResponseEntity.ok().build();
                }
            }
        } else {
            accountService.startPasswordRecovery(userId);
            return ResponseEntity.accepted().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @RequestMapping("check")
    public boolean isUsernameAvailable(@RequestParam("username") String username) {
        return !accountService.hasAccount(username);
    }

}
