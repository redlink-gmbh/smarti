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
 * @ngdoc function
 * @name smartiApp.controller:ConversationEditCtrl
 * @description
 * # ConversationEditCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('ConversationEditCtrl', function ($scope, $location, client, conversation, ConversationService, toastr) {
    var $ctrl = this;

    $ctrl.backToList = backToList;
    $ctrl.saveMessage = saveMessage;
    $ctrl.deleteMessage = deleteMessage;
    $ctrl.setConversationStatus = setConversationStatus;

    function backToList() {
      $location.path('client/' + client.data.id + '/conversations');

    }

    function handleError(error) {
      if (error.status === 501) {
        toastr.warning('This function has not (yet) been implemented', '501 - Not Implemented');
      } else {
        toastr.error(error.data.message, 'Error ' + error.status);
      }
    }

    function saveMessage(message) {
      return ConversationService.saveMessage(conversation.id, message).then(
        function (response) {
          conversation.messages = response.messages;
        },
        handleError
      ).then(function () {
          message.editing = false;
      });
    }

    function deleteMessage(message) {
      return ConversationService.deleteMessage(conversation.id, message.id).then(
        function (response) {
          conversation.messages = response.messages;
          return response;
        },
        handleError
      );
    }

    function setConversationStatus(newStatus) {
      return ConversationService.setConversationStatus(conversation.id, newStatus).then(
        function (response) {
          conversation.meta.status = response.meta.status;
          return response;
        },
        handleError
      );
    }

  });
