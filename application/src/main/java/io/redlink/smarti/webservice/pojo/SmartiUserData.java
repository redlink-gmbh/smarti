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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.services.AuthenticationService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

@ApiModel
public class SmartiUserData {
    @ApiModelProperty(example="user1", notes="the name for the user used to log in")
    private final String login;
    @ApiModelProperty(required=false, notes="The roles for the user (e.g. " + AuthenticationService.ADMIN + ")")
    private Set<String> roles = new HashSet<>();
    @ApiModelProperty(required=true, allowEmptyValue=false, notes="The ids of clients this user is assigned to")
    private Set<String> clients = new HashSet<>();
    @ApiModelProperty(hidden=true)
    private Map<String, String> profile = new HashMap<>();

    @JsonCreator
    public SmartiUserData(@JsonProperty("login") String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public SmartiUserData setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }

    public Set<String> getClients() {
        return clients;
    }

    public SmartiUserData setClients(Set<String> clients) {
        this.clients = clients;
        return this;
    }

    public Map<String, String> getProfile() {
        return profile;
    }

    public SmartiUserData setProfile(Map<String, String> profile) {
        this.profile = profile;
        return this;
    }

    public SmartiUser toModel() {
        final SmartiUser smartiUser = new SmartiUser();
        smartiUser.setLogin(getLogin());
        smartiUser.setProfile(getProfile());
        return smartiUser;
    }

    public static SmartiUserData fromModel(SmartiUser u) {
        return new SmartiUserData(u.getLogin())
                .setRoles(Collections.unmodifiableSet(u.getRoles()))
                .setClients(Collections.unmodifiableSet(
                        u.getClients().stream()
                                .map(ObjectId::toHexString)
                                .collect(Collectors.toSet())
                        )
                )
                .setProfile(Collections.unmodifiableMap(u.getProfile()))
                ;
    }
}
