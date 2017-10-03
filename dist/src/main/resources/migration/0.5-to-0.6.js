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
var conversations = db.getCollection('conversations'),
    configurations = db.getCollection('configuration'),
    clients = db.getCollection('client');

// migrate channel_id and suppport_area to meta.properties (#87, #99)
conversations
    .find({
        'meta.properties': { $exists: false }
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
