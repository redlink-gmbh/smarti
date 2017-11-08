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

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ScriptOperations;
import org.springframework.data.mongodb.core.script.ExecutableMongoScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@EnableConfigurationProperties(MongoDbMigrationProperties.class)
public class MongoDbMigrationService {

    private final Logger log = LoggerFactory.getLogger(MongoDbMigrationService.class);

    private final MongoTemplate mongoTemplate;
    private final MongoDbMigrationProperties migrationProperties;

    @Autowired
    public MongoDbMigrationService(MongoTemplate mongoTemplate,
                                   @SuppressWarnings("SpringJavaAutowiringInspection")
                                           MongoDbMigrationProperties migrationProperties) {
        this.mongoTemplate = mongoTemplate;
        this.migrationProperties = migrationProperties;
    }


    public void runDatabaseMigration() throws IOException {
        final ScriptOperations scriptOps = mongoTemplate.scriptOps();

        if (migrationProperties.getScriptHome() != null) {
            try {
                Files.list(migrationProperties.getScriptHome())
                        .filter(s -> migrationProperties.getPattern().matcher(s.getFileName().toString()).find())
                        .sorted(this::compareCaseInsensitive)
                        .forEachOrdered(s -> runDatabaseMigration(s, scriptOps));
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        } else {
            log.error("smarti.migration.mongo.script-home not set - not running database migration!");
        }
    }

    private void runDatabaseMigration(Path scriptFile, ScriptOperations scriptOps) throws UncheckedIOException {
        log.debug("Running database-migration script {}", scriptFile);
        try {
            final String script = "function() {\n" +
                    new String(Files.readAllBytes(scriptFile), Charsets.UTF_8) +
                    "\n" +
                    "}\n";

            final ExecutableMongoScript migrationScript = new ExecutableMongoScript(script);
            log.trace("About to run migration-script {}:\n{}", scriptFile, migrationScript.getCode());
            scriptOps.execute(migrationScript);
            log.info("Executed database-migration {}", scriptFile);
        } catch (IOException ex) {
            log.debug("Error while executing migration script {}", scriptFile, ex);
            throw new UncheckedIOException(ex);
        }
    }

    private int compareCaseInsensitive(Path a, Path b) {
        final String aName = a.getFileName().toString().toLowerCase();
        final String bName = b.getFileName().toString().toLowerCase();

        return aName.compareTo(bName);
    }
}
