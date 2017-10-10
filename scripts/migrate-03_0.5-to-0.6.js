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
var smarti = db.getCollection('smarti'),
    versionFrom = 2, versionTo = 3;
var conversations = db.getCollection('conversations');

while (true) {
    var dbVersion = smarti.findOne({_id: 'db-version', isTainted: false});
    if (!dbVersion) {
        break;
    } else if (dbVersion.isUpgrading) {
        sleep(250);
        continue;
    } else if (dbVersion.version !== versionFrom) {
        break;
    }
    var lock = smarti.update({_id: 'db-version', version: versionFrom, isUpgrading: false, isTainted: false}, {
        $set: { isUpgrading: true }
    }, { upsert: true });
    if (lock.nModified !== 1) {
        // Could not acquire lock
        continue;
    }
    // Lock acquired
    var start = new ISODate();
    try {
        var result = runDatabaseMigration();
        smarti.update({_id: 'db-version'}, {
            $set: {
                version: versionTo
            },
            $addToSet: {
                history: { version: versionTo, start: start, complete: new ISODate(), result: result, success: true }
            }
        });
    } catch (err) {
        smarti.update({_id: 'db-version'}, {
            $set: { isTainted: true },
            $addToSet: {
                history: { version: versionTo, start: start, complete: new ISODate(), result: err, success: false }
            }
        });
    } finally {
        // release lock
        smarti.update({_id: 'db-version'}, {
            $set: {
                isUpgrading: false
            }
        });
    }
    break;
}

function runDatabaseMigration() {
    // migrate channel_id and support_area to meta.properties (#87, #99)
    conversations
        .find({
            'meta.properties': {$exists: false}
        })
        .forEach(function (c) {
            var metaProps = {};
            if (c.meta && c.meta.tags) {
                metaProps.tags = c.meta.tags;
            }
            if (c.context && c.context.environment) {
                if (c.context.environment.channel_id) {
                    metaProps.channel_id = [c.context.environment.channel_id];
                }
                if (c.context.environment.support_area) {
                    metaProps.support_area = [c.context.environment.support_area];
                }
            }

            conversations.update({
                _id: c._id
            }, {
                $set: {
                    'meta.properties': metaProps
                },
                $unset: {
                    'meta.tags': true
                }
            });
        });

    return 'success';
}