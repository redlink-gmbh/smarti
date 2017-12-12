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
  .controller('LoginCtrl', function ($scope, $location, $routeParams, $log, toastr, UserService) {

    var $ctrl = this;

    $ctrl.action = 'login'; //$routeParams.action; // 'login' as default
    $ctrl.login = login;
    $ctrl.signUp = signup;
    $ctrl.recoverPassword = recoverPassword;
    $ctrl.resetPassword = resetPassword;

    $ctrl.userName = $routeParams.user;
    $ctrl.userPasswd = null;
    $ctrl.userEmail = null;
    $ctrl.recoverAddress = null;
    $ctrl.recoveryToken = null; //$routeParams.token;
    $ctrl.error = $routeParams.error || false;
    if ($ctrl.userName && $ctrl.recoveryToken) {
      $ctrl.action = 'set-password'
    }

    $scope.$watch('$user', function (newVal, oldVal) {
      if (newVal && newVal !== oldVal) {
        gotoStart();
      }
    });

    ///////////////////////////////////////////

    function login() {
      return UserService.login($ctrl.userName, $ctrl.userPasswd)
        .then(function(u) {
          toastr.success('Welcome ' + (u.profile.name || u.login), 'Login successful');
          gotoStart();
        }, function (response) {
          $ctrl.userPasswd = null;
          toastr.error('Login Failed');
        });
    }

    function signup() {
      return UserService.signup($ctrl.userName, $ctrl.userEmail, $ctrl.userPasswd)
        .then(function(u) {
          toastr.success('Welcome ' + (u.profile.name || u.login), 'Sign-Up successful');
          gotoStart();
        }, function (response) {
          $ctrl.userPasswd = null;
          toastr.error('Please try again', 'Sign-Up Failed');
        });
    }

    function recoverPassword() {
      return UserService.recoverPassword($ctrl.recoverAddress)
        .then(function () {
          toastr.success('Please check your mailbox for further instructions', 'Recovery-Token created');
          $ctrl.action = 'set-password';
        }, function (response) {
          toastr.error('Could not initialize password-recovery workflow. Is you mail-address correct?', 'Error');
        });
    }

    function resetPassword() {
      return UserService.completePasswordRecovery($ctrl.userName, $ctrl.userPasswd, $ctrl.recoveryToken)
        .then(function () {
          return login($ctrl.userName, $ctrl.userPasswd);
        }, function (response) {
          $ctrl.userPasswd = null;
          toastr.error('Password reset failed: Did you provide the correct token?', 'Error')
        });
    }

    function gotoStart() {
      $location.path('/').search({})
    }

  });
