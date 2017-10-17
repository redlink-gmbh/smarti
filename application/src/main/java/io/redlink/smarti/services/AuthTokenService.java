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
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

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

        return authTokenRepository.save(new AuthToken().setLabel(label).setClientId(clientId));
    }

    public boolean deleteAuthToken(String token, ObjectId clientId) {
        if (authTokenRepository.existsByTokenAndAndClientId(token, clientId)) {
            authTokenRepository.deleteByTokenAndClientId(token, clientId);
            return true;
        } else {
            return false;
        }
    }

    public AuthToken updateAuthToken(String tokenId, ObjectId userId, AuthToken token) {
        final AuthToken stored = authTokenRepository.findOneByTokenAndClientId(tokenId, userId);
        if (token == null) return stored;
        if (stored == null) {
            return null;
        } else {
            stored.setLabel(StringUtils.defaultString(token.getLabel(), stored.getLabel()));

            return authTokenRepository.save(stored);
        }
    }
}
