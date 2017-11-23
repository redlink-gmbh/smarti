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

import io.redlink.smarti.model.AuthToken;
import io.redlink.smarti.repositories.AuthTokenRepository;
import io.redlink.utils.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;

    public AuthTokenService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    public List<AuthToken> getAuthTokens(ObjectId clientId) {
        return authTokenRepository.findByClientId(clientId);
    }

    public AuthToken createAuthToken(ObjectId clientId, String label) {
        label = StringUtils.defaultString(label, "new-token");

        final AuthToken authToken = authTokenRepository.save(new AuthToken(clientId).setLabel(label));
        authToken.setSecret(RandomUtils.nextString(8));
        return authTokenRepository.save(authToken);
    }

    public boolean deleteAuthToken(String token, ObjectId clientId) {
        if (authTokenRepository.existsByIdAndAndClientId(token, clientId)) {
            authTokenRepository.deleteByIdAndClientId(token, clientId);
            return true;
        } else {
            return false;
        }
    }

    public AuthToken updateAuthToken(String tokenId, ObjectId userId, AuthToken token) {
        final AuthToken stored = authTokenRepository.findOneByIdAndClientId(tokenId, userId);
        if (token == null) return stored;
        if (stored == null) {
            return null;
        } else {
            stored.setLabel(StringUtils.defaultString(token.getLabel(), stored.getLabel()));

            return authTokenRepository.save(stored);
        }
    }

    public ObjectId getClientId(String authToken) {
        final AuthToken client = authTokenRepository.findOneByToken(authToken);
        return Objects.nonNull(client)?client.getClientId():null;
    }
}
