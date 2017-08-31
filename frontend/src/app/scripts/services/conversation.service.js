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

'use strict';

/**
 * @ngdoc service
 * @name smartiApp.ConversationService
 * @description
 * # ConversationService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('ConversationService', function ($http, $q, ENV) {

    this.listForClient = getForClient;
    this.getConversation = getConversation;

    this.saveMessage = saveMessage;
    this.deleteMessage = deleteMessage;

    function getForClient(clientId, page, pageSize) {
      return $http.get(ENV.serviceBaseUrl + 'admin/conversation', {
        params: {
          clientId: clientId,
          page: page,
          pageSize: pageSize
        }
      }).then(function (response) {
        return response.data;
      });
    }

    function getConversation(conversationId) {
      return $http.get(ENV.serviceBaseUrl + 'admin/conversation/' + conversationId)
        .then(function (response) {
          return response.data;
        });
    }

    function saveMessage(conversationId, message) {
      return $http.put(ENV.serviceBaseUrl + 'admin/conversation/' + conversationId + '/message/' + message.id, message)
        .then(function (response) {
          return response.data;
        });
    }

    function deleteMessage(conversationId, messageId) {
      return $http.delete(ENV.serviceBaseUrl + 'admin/conversation/' + conversationId + '/message/' + messageId)
        .then(function (response) {
          return response.data;
        });
    }

  });
