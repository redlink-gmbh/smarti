'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:OverviewCtrl
 * @description
 * # OverviewCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('OverviewCtrl', function ($scope, $location, ClientService) {

    ClientService.list().then(function(clients){
       $scope.clients = clients;
    });

    $scope.edit = function(clientId) {
      $location.path('client/' + clientId);
    };

    $scope.createClient = function() {
      $location.path('client');
    };

  });
