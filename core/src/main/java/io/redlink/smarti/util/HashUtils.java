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

package io.redlink.smarti.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @deprecated use {@link io.redlink.utils.HashUtils} instead.
 */
@Deprecated
public class HashUtils {

    private HashUtils() {
        throw new AssertionError("No io.redlink.reisebuddy.util.HashUtils instances for you!");
    }

    public static String sha256sum(String string) {
        return calcHash(string, "SHA-256");
    }

    private static String calcHash(String string, String algo) {
        try {
            return calcHash(string.getBytes("UTF-8"), algo);
        } catch (UnsupportedEncodingException e) {
            return calcHash(string.getBytes(), algo);
        }
    }
    private static String calcHash(byte[] bytes, String algorithm) {
        try {
            MessageDigest m = MessageDigest.getInstance(algorithm);
            m.update(bytes);
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
