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
package io.redlink.smarti.health;

import com.google.common.collect.ImmutableMap;
import com.mongodb.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class MongoHealthCheck extends AbstractHealthIndicator {

    public static final int EXPECTED_DB_VERSION = 6;

    public static final String SMARTI_DB_VERSION_ID = "db-version";
    public static final String COLLECTION_NAME = "smarti";

    private final Logger log = LoggerFactory.getLogger(MongoHealthCheck.DbVersion.class);
    private final MongoTemplate mongoTemplate;

    public MongoHealthCheck(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    protected void checkDatabaseVersion() {
        final DbVersion dbVersion = mongoTemplate.findById(SMARTI_DB_VERSION_ID, DbVersion.class, COLLECTION_NAME);
        if (dbVersion == null) {
            // looks like an empty database
            log.info("No {} found in {}, creating new entry with version {}", SMARTI_DB_VERSION_ID, COLLECTION_NAME, EXPECTED_DB_VERSION);
            mongoTemplate.insert(createDbVersion(), COLLECTION_NAME);
        } else if (dbVersion.isTainted) {
            log.error("Refusing to start with tainted database: {}", dbVersion);
            throw new IllegalStateException("Refusing to run with tainted database");
        } else if (dbVersion.isUpgrading) {
            log.error("Refusing to start while database-upgrade is in progress");
            throw new IllegalStateException("Refusing to start while database-upgrade is in progress");
        } else if (dbVersion.version != EXPECTED_DB_VERSION) {
            log.error("Database has wrong version! Expected: {}, found: {}", EXPECTED_DB_VERSION, dbVersion.version);
            throw new IllegalStateException(String.format("Found incompatible db-version %d, expected: %d", dbVersion.version, EXPECTED_DB_VERSION));
        } else {
            log.info("Connected to smarti-database, version {}", dbVersion.version);
            if (Objects.nonNull(dbVersion.backup) && !dbVersion.backup.isEmpty()) {
                log.info("Removing reference backup {} (created: {}, version: {})",
                        dbVersion.backup.get("name"),
                        dbVersion.backup.get("date"),
                        dbVersion.backup.get("version"));
                mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(SMARTI_DB_VERSION_ID)), new Update().unset("backup"), COLLECTION_NAME);
            }
        }
    }

    private DbVersion createDbVersion() {
        final Date now = new Date();
        return new DbVersion(SMARTI_DB_VERSION_ID)
                .setVersion(EXPECTED_DB_VERSION)
                .setHistory(Collections.singletonList(
                        ImmutableMap.of(
                                "version", EXPECTED_DB_VERSION,
                                "start", now,
                                "complete", now,
                                "result", "initialized",
                                "success", true
                        )
                ));
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        final CommandResult buildInfo = mongoTemplate.executeCommand("{ buildInfo: 1 }");

        final DbVersion dbVersion = mongoTemplate.findById(SMARTI_DB_VERSION_ID, DbVersion.class, COLLECTION_NAME);

        builder.up()
                .withDetail("version", buildInfo.getString("version"))
                .withDetail("db-version", dbVersion.version)
        ;

    }


    public static class DbVersion {
        @Id
        private final String id;
        private int version = 0;
        private boolean isUpgrading = false, isTainted = false;
        private List<Map<String,Object>> history;
        private Map<String,Object> backup;

        @PersistenceConstructor
        public DbVersion(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public int getVersion() {
            return version;
        }

        public DbVersion setVersion(int version) {
            this.version = version;
            return this;
        }

        public boolean isUpgrading() {
            return isUpgrading;
        }

        public DbVersion setUpgrading(boolean upgrading) {
            isUpgrading = upgrading;
            return this;
        }

        public boolean isTainted() {
            return isTainted;
        }

        public DbVersion setTainted(boolean tainted) {
            isTainted = tainted;
            return this;
        }

        public List<Map<String, Object>> getHistory() {
            return history;
        }

        public DbVersion setHistory(List<Map<String, Object>> history) {
            this.history = history;
            return this;
        }

        public Map<String, Object> getBackup() {
            return backup;
        }

        public DbVersion setBackup(Map<String, Object> backup) {
            this.backup = backup;
            return this;
        }
    }
}
