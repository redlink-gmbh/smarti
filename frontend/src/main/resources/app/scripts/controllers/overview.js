'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('OverviewCtrl', function ($scope, ClientService) {

    ClientService.list().then(function(data){
       $scope.clients = data.data;
    });

    $scope.edit = function(clientId) {
      $location.path('client/' + clientId);
    }

  });
