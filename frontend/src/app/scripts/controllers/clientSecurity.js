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
 * @name smartiApp.controller:ClientSecurityCtrl
 * @description
 * # ClientSecurityCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('ClientSecurityCtrl', function ($scope, $location, client, ClientService) {
    var $ctrl = this;

    $scope.client = client.data;
    $scope.authTokens = [];
    $scope.users = [];

    $ctrl.backToList = backToList;
    $ctrl.createAuthToken = createAuthToken;
    $ctrl.revokeAuthToken = revokeAuthToken;
    $ctrl.updateAuthToken = updateAuthToken;

    init();

    /////////////////////////////////////

    function init() {
      loadAuthTokens();
    }

    function backToList() {
      $location.path('/');
    }

    function loadAuthTokens() {
      return ClientService.loadAuthTokens(client)
        .then(function (tokens) {
          $scope.authTokens = tokens;
        });
    }

    function createAuthToken() {
      return ClientService.createAuthToken(client, undefined)
        .then(function (newToken) {
          $scope.authTokens.push(newToken);
          return newToken;
        });
    }

    function revokeAuthToken(token) {
      return ClientService.revokeAuthToken(client, token)
        .then(function () {
          return $scope.authTokens = $scope.authTokens.filter(function(t) {
            return t.token !== token.token;
          });
        });
    }

    function updateAuthToken(token) {
      return ClientService.updateAuthToken(client, token)
        .then(function (updatedToken) {
          return $scope.authTokens = $scope.authTokens.map(function (t) {
            if (t.id === updatedToken.id) {
              return updatedToken;
            }
            return t;
          })
        });
    }

  });
