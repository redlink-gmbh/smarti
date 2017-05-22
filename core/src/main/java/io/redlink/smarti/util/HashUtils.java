/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 */
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
