'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:OverviewCtrl
 * @description
 * # OverviewCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('OverviewCtrl', function ($scope, $window, $location, ClientService) {

    function listClients() {
      ClientService.list().then(function(clients){
        $scope.clients = clients;
      })
    }

    $scope.edit = function(clientId) {
      $location.path('client/' + clientId);
    };

    $scope.clone = function(clientId) {
      $location.path('client/' + clientId).search('clone');
    };

    $scope.createClient = function() {
      $location.path('client');
    };

    $scope.saveClient = function(client) {
      client.save().then(listClients);
    };

    $scope.delete = function(client) {
      if($window.confirm("Do you realy want to delete " + client.data.name + "?")) {
        client.delete().then(listClients);
      }
    };

    listClients();

  });
