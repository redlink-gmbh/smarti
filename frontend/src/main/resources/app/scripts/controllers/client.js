'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:ClientCtrl
 * @description
 * # ClientCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('ClientCtrl', function ($scope,client) {
     $scope.client = client;
  });
