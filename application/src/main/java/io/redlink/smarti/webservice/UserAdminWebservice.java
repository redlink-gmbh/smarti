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
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.UserService;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/admin/users", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@ConditionalOnBean(MongoUserDetailsService.class)
@Api
public class UserAdminWebservice {

    private final AuthenticationService authenticationService;

    private final UserService userService;

    @Autowired
    public UserAdminWebservice(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<?> listUsers(AuthContext authContext, @RequestParam(name = "filter", required = false) String filter) {
        authenticationService.assertRole(authContext, AuthenticationService.ADMIN);

        return userService.listUsers(filter);
    }

}
