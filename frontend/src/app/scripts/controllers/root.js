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
 * @name smartiApp.controller:RootCtrl
 * @description
 * # RootCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('RootCtrl', function ($rootScope, $location, $log, UserService) {

    var rootCtrl = this,
      restActive = false;

    rootCtrl.logout = logout;

    fetchUser();
    $rootScope.$on('$routeChangeStart', function($event, next, current) {
      if (!$rootScope.$user) {
        if (next.$$route.controller !== "LoginCtrl") {
          $log.debug(next);
          $event.preventDefault();
          $location.path('/login').search({error:'login-required'});
        }
      }
    });

    ////////////////////

    function logout() {
      return UserService.logout().then(function () {
        return $location.path('/login').search({})
      });

    }

    function fetchUser() {
      restActive = true;
      return UserService.getUser()
        .then(
          function (user) {
            if (user) {
              $log.debug('Current User is ' + user.login);
            } else {
              $log.debug('No user!')
            }
            $rootScope.$user = user;
          },
          function (err) {
            $log.debug('Could not retrieve user');
            $rootScope.$user = null;
          }
        )
        .finally(function () {
          restActive = false;
          onUserChange($rootScope.$user);
          $rootScope.$watch('$user', onUserChange);
        })
    }

    function onUserChange(user) {
      if (!restActive && !user) {
        $location.path('/login').search({});
      }
    }

  });
