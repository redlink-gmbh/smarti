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
        		"queryBuilder" : [
        			{
        				"name" : "conversationmlt",
        				"displayName" : "Related Conversations",
        				"type" : "conversationmlt",
        				"enabled" : true,
        				"unbound" : false,
        				"configuration" : {
        					
        				}
        			},
        			{
        				"solrEndpoint" : "http://host.domain.org:8983/solr/change-me",
        				"search" : {
        					"title" : {
        						"enabled" : false,
        						"field" : ""
        					},
        					"fullText" : {
        						"enabled" : true,
        						"field" : ""
        					},
        					"spatial" : {
        						"enabled" : true,
        						"locationNameField" : ""
        					},
        					"temporal" : {
        						"enabled" : false
        					},
        					"related" : {
        						"enabled" : false,
        						"fields" : [ ]
        					}
        				},
        				"defaults" : {
        					"rows" : 10,
        					"fields" : "*,score"
        				},
        				"result" : {
        					"mappings" : {
        						"title": "title",
        				        "description": "description",
        				        "type": "type",
        				        "doctype": "doctype",
        				        "thumb": "thumb",
        				        "link": "link",
        				        "date": "date",
        				        "source": "source"
        					},
        				},
        				"name" : "db-search-endpoint",
        				"displayName" : "DB Search Endpoint",
        				"type" : "solrsearch",
        				"enabled" : false,
        				"unbound" : false,
        				"configuration" : {
        					
        				},
        				"_class" : "io.redlink.smarti.query.solr.SolrEndpointConfiguration"
        			},
        			{
        				"name" : "conversationsearch",
        				"displayName" : "conversationsearch",
        				"type" : "conversationsearch",
        				"enabled" : true,
        				"unbound" : false,
        				"configuration" : {

        				}
        			}
        		]
        	}
        }},
        { upsert: true }
    );
});
