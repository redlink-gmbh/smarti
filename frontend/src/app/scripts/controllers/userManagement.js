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

    $ctrl.createUser = createUser;
    $ctrl.editUser = editUser;
    $ctrl.setPassword  = setPassword;
    $ctrl.deleteUser = deleteUser;
    init();

    ///////////////////////////

    function init() {
      UserService.listUsers()
        .then(function (users) {
          $scope.users = users;
        });
    }

    function createUser() {
      return $uibModal.open({
        scope: $scope,
        templateUrl: 'views/modal/create-user.html'
      }).result
        .then(
          function (newUser) {
            return UserService.createUser(newUser)
              .then(function (created) {
                return UserService.setPassword(created, newUser.password)
                  .finally(function () {
                    $scope.users.push(created);
                    return created;
                  });
              });
          },
          function () {
            // modal canceled
          }
        );

    }

    function editUser(user) {
      return $uibModal.open({
        scope: $scope,
        templateUrl: 'views/modal/admin-edit-user.html',
        resolve: {
          user: angular.copy(user)
        },
        controller: function () {}
      }).result
        .then(
          function (updatedUser) {
            var roles = [];
            if (updatedUser.isAdmin) {
              roles.push('ADMIN');
            }
            return UserService.updateUser(updatedUser)
              .then(function (savedUser) {
                return UserService.setRoles(savedUser, roles);
              })
              .then(function (savedUser) {
                $scope.users = $scope.users.map(function (u) {
                  if (u.login === savedUser.login) {
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
        },
        controller: function () {}
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

    function deleteUser(user) {
      return UserService.deleteUser(user)
        .then(function() {
          $scope.users = $scope.users.filter(function(u) {
            return u.login !== user.login;
          });
        });
    }


  });
