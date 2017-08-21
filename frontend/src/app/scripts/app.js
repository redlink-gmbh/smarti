'use strict';

/**
 * @ngdoc overview
 * @name smartiApp
 * @description
 * # smartiApp
 *
 * Main module of the application.
 */
angular
  .module('smartiApp', [
    'config',
    'ngRoute',
    'ui.codemirror'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/overview.html',
        controller: 'OverviewCtrl',
        controllerAs: 'overview'
      })
      .when('/client/:id?', {
        templateUrl: 'views/client.html',
        controller: 'ClientCtrl',
        resolve: {
          client:function(ClientService,Client,$route){
            return $route.current.params.id ?
              ClientService.getById($route.current.params.id) :
              new Client();
          }}
      })
      .otherwise({
        redirectTo: '/'
      });
  });
