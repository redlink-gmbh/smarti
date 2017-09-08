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
 * @name smartiApp.controller:ConversationCtrl
 * @description
 * # ConversationCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('ConversationCtrl', function ($scope, $location, $route, $uibModal, client, ConversationService) {
    var $ctrl = this;

    $ctrl.conversations = null;
    $ctrl.paging = {
      currentPage: 1,
      pageSize: 10,
      totalItems: 0
    };

    $ctrl.openConversation = openConversation;
    $ctrl.backToList = backToList;
    $ctrl.import = importConversations;

    $scope.downloadLink = ConversationService.buildExportLink(client.data.id);

    $scope.$watch('$ctrl.paging.currentPage', loadConversations);
    $scope.$watch('$ctrl.paging.pageSize', loadConversations);

    loadConversations();

    function backToList() {
      $location.path('/');
    }

    function loadConversations() {
      $ctrl.conversations = null;
      return ConversationService.listForClient(client.data.id, $ctrl.paging.currentPage -1, $ctrl.paging.pageSize)
        .then(function (response) {
          $ctrl.paging.currentPage = response.number + 1;
          $ctrl.paging.pageSize = response.size;
          $ctrl.paging.totalItems = response.totalElements;
          $ctrl.conversations = response.content;

          return response;
        });
    }

    function openConversation(conversationId) {
      $location.path('client/' + client.data.id + '/conversations/' + conversationId);
    }

    function importConversations() {
      return $uibModal.open({
        templateUrl: 'views/modal/import-conversations.html',
        controller: ['$uibModalInstance', function ($uibModalInstance) {
          const $modal = this;

          $modal.inProgress = false;
          $modal.upload = function ($form) {
            $modal.serverError = null;
            $form.$setPristine();
            $form.$setSubmitted();
            $modal.inProgress = true;
            ConversationService.importConversations(client.data.id, $modal.uploadFile, $modal.uploadReplace)
              .then(
                $uibModalInstance.close,
                function (error) {
                  $modal.serverError = error.data.message;
                })
              .finally(function () {
                $modal.inProgress = false;
              });
          };
          $modal.cancel = $uibModalInstance.dismiss;

        }],
        controllerAs: '$modal'
      }).result
        .then(
          function() {
            $route.reload();
          },
          function () {

          });
    }

  });
