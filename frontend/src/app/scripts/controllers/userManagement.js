'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:UsermanagementctrlCtrl
 * @description
 * # UsermanagementctrlCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('UserManagementCtrl', function ($scope, UserService) {
    var $ctrl = this;

    $scope.users = [];

    init();

    ///////////////////////////

    function init() {
      UserService.listUsers()
        .then(function (users) {
          $scope.users = users;
        });
    }

  });
