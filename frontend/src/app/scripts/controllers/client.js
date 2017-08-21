'use strict';

/**
 * @ngdoc function
 * @name smartiApp.controller:ClientCtrl
 * @description
 * # ClientCtrl
 * Controller of the smartiApp
 */
angular.module('smartiApp')
  .controller('ClientCtrl', function ($scope,$window,$location,client,ClientService,ConfigurationService) {

    ConfigurationService.getConfiguration(client).then(function(configuration){
       $scope.client = client;
       $scope.configuration = configuration;
    });

    $scope.saveClient = function() {
       $scope.client.save().then(function(client){
         $scope.client = client;
         $scope.configuration.save(client).then(function(){
           $location.path('/');
         }, function(error){
           $window.alert(error.data.message);
         });
       }, function(error){
           $window.alert(error.message);
       });
    };

    $scope.loadDefaultConfiguration = function() {
      ConfigurationService.getDefaultConfiguration().then(function(configuration){
        $scope.configuration = configuration;
      });
    };

    $scope.editorOptions = {
      lineWrapping : true,
      lineNumbers: true,
      mode: 'javascript'
    };

  });
