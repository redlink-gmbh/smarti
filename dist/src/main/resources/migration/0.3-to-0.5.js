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
var conversation = 'conversations',
    configuration = 'configuration',
    client = 'client';

// create all clients
db.getCollection(conversation).aggregate([
    { $project: { _id: '$context.domain' } },
    { $group: { _id: '$_id'}},
    { $project: {
        _id: { $literal: new ObjectId() },
        name: '$_id',
        lastUpdate: { $literal: ISODate() },
        defaultClient: { $literal: false }
    } },
    { $out: client}
]);
db.getCollection(client).find().forEach(function(c) {
    db.getCollection(conversation).update(
        { 'context.domain': c.name },
        { $set: { owner: c._id }},
        { multi: true }
    );
    db.getCollection(configuration).update(
        { client: c._id },
        { $setOnInsert: {
            created: new ISODate(),
            modified: new ISODate(),
            config: {
                // TODO(westei): add default config here!
            }
        }},
        { upsert: true }
    );
});
