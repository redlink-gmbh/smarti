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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Various String helpers
 */
public class StringUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * provides the slug name for the parsed input
     * @param input
     * @return
     */ //from https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls
    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ROOT);
    }

    public static boolean equalsAny(String needle, String... haystack) {
        return Arrays.stream(haystack)
                .anyMatch(h -> org.apache.commons.lang3.StringUtils.equals(needle, h));
    }

    public static boolean equalsAnyIgnoreCase(String needle, String... haystack) {
        return Arrays.stream(haystack)
                .anyMatch(h -> org.apache.commons.lang3.StringUtils.equalsIgnoreCase(needle, h));
    }
}
