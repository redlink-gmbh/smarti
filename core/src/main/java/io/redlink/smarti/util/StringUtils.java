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
