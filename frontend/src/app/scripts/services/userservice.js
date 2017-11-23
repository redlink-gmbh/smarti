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

    this.listUsers = listUsers;
    this.getUser = getUser;
    this.createUser = createUser;
    this.updateUser = updateUser;
    this.deleteUser = deleteUser;

    this.login = login;
    this.logout = logout;
    this.signup = signup;
    this.recoverPassword = recoverPassword;
    this.completePasswordRecovery = completePasswordRecovery;

    this.setPassword = setPassword;
    this.setRoles = setRoles;

    this.checkUsernameExists = checkUsernameExists;

    ///////////////////////////

    function getUser() {
      return login();
    }

    /**
     * @param {object} user
     * @returns {user}
     */
    function enhanceUser(user) {
      user.roles = user.roles || [];

      user.hasRole = function (role) {
        return user.roles.indexOf(role.toUpperCase()) >= 0;
      };
      user.isAdmin = user.hasRole("ADMIN");

      return user;
    }

    function listUsers() {
      return $http.get(ENV.serviceBaseUrl + '/user')
        .then(function (response) {
          return response.data.map(enhanceUser);
        });
    }
    function createUser(user) {
      return $http.post(ENV.serviceBaseUrl + '/user', user)
        .then(function (response) {
          return enhanceUser(response.data);
        });
    }
    function updateUser(user) {
      return $http.put(ENV.serviceBaseUrl + '/user/' + user.login, user)
        .then(function (response) {
          return enhanceUser(response.data);
        });
    }
    function deleteUser(user) {
      var username = user.login || user;
      return $http.delete(ENV.serviceBaseUrl + '/user/' + username)
        .then(function () {
          return true;
        });
    }

    function login(username, password) {
      var headers = {};
      if (username && password) {
        headers.authorization = 'Basic ' + btoa(username+':'+password);
      }

      return $http
        .get(ENV.serviceBaseUrl + '/auth', {
          headers: headers
        })
        .then(
          function (response) {
            if (response.data.login) {
              $rootScope.$user = enhanceUser(response.data);
              return response.data;
            } else {
              $rootScope.$user = null;
              return null;
            }
          },
          function (err) {
            $rootScope.$user = null;
            return null;
          }
        )
        .then(function (login) {
          return $http.get(ENV.serviceBaseUrl + '/user/' + login.login)
            .then(function (response) {
              if (response.data.login) {
                $rootScope.$user = enhanceUser(response.data);
                return $rootScope.$user;
              } else {
                $rootScope.$user = null;
                return null;
              }
            });
        })
    }

    function logout() {
      return $http.post(ENV.serviceBaseUrl + '/logout', undefined)
        .finally(
          function () {
            $rootScope.$user = null;
          }
        );
    }

    function signup(username, email, password) {
      return $http.post(ENV.serviceBaseUrl + '/auth/signup', {
        username: username,
        password: password,
        email: email
      })
        .then(function (reponse) {
          return reponse.data;
        });
    }

    function recoverPassword(email) {
      return $http.post(ENV.serviceBaseUrl + '/auth/recover', undefined, {
        params: {
          user: email
        }
      });
    }

    function completePasswordRecovery(user, password, token) {
      return $http
        .post(ENV.serviceBaseUrl + '/auth/recover', {
          token: token,
          password: password
        }, {
          params: {
            user: user
          }
        })
        .then(function () {
          return login(user, password)
        });
    }

    function setPassword(user, newPassword) {
      var username = user.login || user;
      return $http
        .put(ENV.serviceBaseUrl + '/user/' + username + '/password', {
          password: newPassword
        })
        .then(function (response) {
          return enhanceUser(response.data);
        });
    }
    function setRoles(user, newRoles) {
      var username = user.login || user;
      return $http
        .put(ENV.serviceBaseUrl + '/user/' + username + '/roles', newRoles || [])
        .then(function (response) {
          return enhanceUser(response.data);
        });
    }

    function checkUsernameExists(login) {
      return $http
        .get(ENV.serviceBaseUrl + '/auth/check', {
          params: {
            login: login
          }
        })
        .then(function (response) {
          return response.data;
        });
    }

  });
