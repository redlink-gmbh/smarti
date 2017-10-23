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
var rocketChatMongoDbServer = new Mongo('localhost:27017'),
    smartiMongoDbServer = new Mongo('localhost:27017');

var rocketChatDB = rocketChatMongoDbServer.getDB('rocketchat'),
    smartiDB = smartiMongoDbServer.getDB('smarti');

/* optional - login to the databases
// TODO: replace username and password with the correct values
rocketChatDB.auth('username', 'password');
smartiDB.auth('username', 'password');
 */

var support_area_table = {};
rocketChatDB.getCollection('rocketchat_room')
    .find({t: 'r'}, {expertise: 1})
    .forEach(function (r) {
        support_area_table[r._id] = r.expertise;
    });

var conversationCollection = smartiDB.getCollection('conversations');
conversationCollection
    .find({
        'context.environment.channel_id': { $exists: true },
        'context.environment.support_area': null
    })
    .forEach(function (c) {
        var support_area = support_area_table[c.context.environment.channel_id];
        if (support_area) {
            conversationCollection.update({
                _id: c._id
            }, {
                $set: {
                    'context.environment.support_area': support_area
                }
            });
        }
    });
