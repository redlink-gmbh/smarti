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
package io.redlink.smarti.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "smarti.migration.mongo")
public class MongoDbMigrationProperties {

    private Path scriptHome = null;
    private Pattern pattern = Pattern.compile("^migrate-\\d+");

    public Path getScriptHome() {
        return scriptHome;
    }

    public void setScriptHome(Path scriptHome) {
        this.scriptHome = scriptHome;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }
}
