/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.util;

import java.util.Arrays;

/**
 * Various String helpers
 */
public class StringUtils {

    public static boolean equalsAny(String needle, String... haystack) {
        return Arrays.stream(haystack)
                .anyMatch(h -> org.apache.commons.lang3.StringUtils.equals(needle, h));
    }

    public static boolean equalsAnyIgnoreCase(String needle, String... haystack) {
        return Arrays.stream(haystack)
                .anyMatch(h -> org.apache.commons.lang3.StringUtils.equalsIgnoreCase(needle, h));
    }
}
