'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:UsermanagementctrlCtrl
 * @description
 * # UsermanagementctrlCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('UserManagementCtrl', function ($scope, $uibModal, toastr, UserService) {
    var $ctrl = this;

    $scope.users = [];

    $ctrl.editUser = editUser;
    $ctrl.setPassword  = setPassword;

    init();

    ///////////////////////////

    function init() {
      UserService.listUsers()
        .then(function (users) {
          $scope.users = users;
        });
    }

    function editUser(user) {
      return $uibModal.open({
        scope: $scope,
        templateUrl: 'views/modal/admin-edit-user.html',
        resolve: {
          user: angular.copy(user)
        },
        controller: function ($scope, user) {
          $scope.user = user;
        }
      }).result
        .then(
          function (updatedUser) {
            updatedUser.roles = updatedUser.roles || [];
            if (updatedUser.isAdmin) {
              updatedUser.roles.push('ADMIN');
            }
            return UserService.updateUser(updatedUser)
              .then(function (savedUser) {
                $scope.users = $scope.users.map(function (u) {
                  if (u.username === savedUser.username) {
                    return savedUser;
                  } else {
                    return u;
                  }
                })
              }, function (error) {
                toastr.error(error);
              });
          },
          function () {
            // modal canceled
          }
        );
    }

    function setPassword(user) {
      return $uibModal.open({
        scope: $scope,
        templateUrl: 'views/modal/update-password.html',
        resolve: {
          user: angular.copy(user)
        }
      }).result.then(
        function (newPassword) {
          return UserService.setPassword(user, newPassword)
            .then(function (success) {
              return success;
            },
              function (error) {
                toastr.error(error);
              });
        },
        function () {
          // modal canceled
        }
      );
    }


  });
