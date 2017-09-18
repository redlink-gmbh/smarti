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
package io.redlink.smarti.utils;

import io.redlink.smarti.exception.HttpBadRequestException;

import java.text.MessageFormat;

public class WebserviceUtils {
    public static void checkParameter(boolean check, String message) {
        checkParameter(check, "{0}", message);
    }

    public static void checkParameter(boolean check, String messageTemplate, Object... args) {
        if (!check) {
            throw new HttpBadRequestException(MessageFormat.format(messageTemplate, args));
        }
    }
}
