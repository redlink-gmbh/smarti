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
    this.setPassword = setPassword;

    this.checkUsernameAvailable = checkUsernameAvailable;

    ///////////////////////////

    function getUser() {
      return login();
    }

    function enhanceUser(user) {

      user.hasRole = function (role) {
        return user.roles.indexOf(role.toUpperCase()) >= 0;
      };
      user.isAdmin = user.hasRole("ADMIN");

      return user;
    }

    function login(username, password) {
      var headers = {};
      if (username && password) {
        headers.authorization = 'Basic ' + btoa(username+':'+password);
      }

      return $http.get(ENV.serviceBaseUrl + '/auth', {
        headers: headers
      }).then(
        function (response) {
          if (response.data.name) {
            $rootScope.user = enhanceUser(response.data);
            return response.data;
          } else {
            $rootScope.user = null;
            return null;
          }
        },
        function (err) {
          $rootScope.user = null;
          return null;
        }
      )
    }

    function logout() {
      return $http.post(ENV.serviceBaseUrl + '/logout', undefined)
        .finally(
          function () {
            $rootScope.user = null;
          }
        );
    }

    function signup(username, email, password) {
      return $http.post(ENV.serviceBaseUrl + '/auth', {
        username: username,
        password: password,
        email: email
      })
        .then(function (reponse) {
          return reponse.data;
        });
    }

    function recoverPassword(email) {
      return $q.reject('Not implemented');
    }

    function setPassword(user, newPassword) {
      var username = user.username || user;
      return $http
        .put(ENV.serviceBaseUrl + '/auth/' + username + '/password', {
          password: newPassword
        })
        .then(function (response) {
          return response.data;
        });
    }

    function checkUsernameAvailable(username) {
      return $http
        .get(ENV.serviceBaseUrl + '/auth/check', {
          params: {
            username: username
          }
        })
        .then(function (response) {
          return response.data;
        });
    }

  });
