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

import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.SmartiUser;
import io.redlink.smarti.repositories.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean existsUsername(String username) {
        return userRepository.exists(username);
    }

    public List<SmartiUser> getUsersForClient(Client client) {
        return userRepository.findByClientsContains(client.getId());
    }

    public SmartiUser createUserForClient(SmartiUser user, Client client) {
        user.getClients().add(client.getId());
        return userRepository.create(user);
    }

    public Set<ObjectId> getClientsForUser(String username) {
        final SmartiUser user = userRepository.findOne(username);
        if (user == null) return Collections.emptySet();
        return user.getClients();
    }

    public void removeUserFromClient(String username, Client client) {
        userRepository.removeClient(username, client.getId());
    }

    public SmartiUser addUserToClient(String username, Client client) {
        return userRepository.addClient(username, client.getId());
    }

    public List<? extends SmartiUser> listUsers(String filter) {
        return userRepository.findAllWithFilter(filter);
    }

    public SmartiUser getUser(String username) {
        return userRepository.findOne(username);
    }
}
