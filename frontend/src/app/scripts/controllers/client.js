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
      if(client.isCopy) {
        client.data.id = null;
        client.data.name = null;
        client.data.description = null;
      }
       $scope.client = client;
       $scope.configuration = configuration;
    });

    ConfigurationService.getDefaultConfiguration().then(function(configuration){
      $scope.defaultConfiguration = configuration;
    });

    $scope.saveClient = function() {
       $scope.client.save().then(function(client){
         $scope.client = client;
         $scope.configuration.save(client).then(function(){
           $location.path('/').search('clone',null);
         }, function(error){
           $window.alert(error.data.message);
         });
       }, function(error){
           $window.alert(error.message);
       });
    };

    $scope.cancel = function() {
      $location.path('/').search('clone',null);
    };

    $scope.addComponent = function(type,component) {
      $scope.configuration.addComponent(type,component.clone());
    };

    $scope.removeComponent = function(type,component) { console.log(component);
      if($window.confirm("Do you really want to remove component " + component.innerData.displayName)) {
        $scope.configuration.removeComponent(type,component);
      }
    };

    $scope.editorOptions = {
      lineWrapping : true,
      lineNumbers: true,
      mode: 'javascript'
    };

  });
