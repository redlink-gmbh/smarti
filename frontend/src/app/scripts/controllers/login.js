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
 * @name smartiApp.controller:OverviewCtrl
 * @description
 * # OverviewCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('LoginCtrl', function ($scope, $location) {

    var $ctrl = this;

    $ctrl.action = null; // 'login' as default
    $ctrl.login = login;
    $ctrl.recoverPassword = recoverPassword;

    if ($scope.user) {
      gotoStart();
    }

    ///////////////////////////////////////////

    function login() {
      if ($ctrl.action === 'signup') {
        return signup();
      }
    }

    function signup() {

    }

    function recoverPassword() {

    }

    function gotoStart() {
      $location.path('/')
    }
  });
