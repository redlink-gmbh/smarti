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
 * @name smartiApp.ClientService
 * @description
 * # ClientService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('UserService', function ($rootScope, $http, $q, ENV) {

    this.getUser = getUser;
    this.login = login;
    this.logout = logout;
    this.signup = signup;
    this.recoverPassword = recoverPassword;

    ///////////////////////////

    function getUser() {
     return login();
    }

    function login(username, password) {
      var headers = {};
      if (username && password) {
        headers.authorization = 'Basic ' + btoa(username+':'+password);
      }

      return $http.get(ENV.serviceBaseUrl + 'user', {
        headers: headers
      }).then(
        function (response) {
          $rootScope.user = response.data;
          return response.data;
        },
        function (err) {
          $rootScope.user = null;
          return null;
        }
      )
    }

    function logout() {
      return $http.post(ENV.serviceBaseUrl + 'logout', undefined)
        .finally(
          function () {
            $rootScope.user = null;
          }
        );
    }

    function signup(username, email, password) {
      return $q.reject('Not implemented');
    }

    function recoverPassword(email) {
      return $q.reject('Not implemented');
    }


  });
