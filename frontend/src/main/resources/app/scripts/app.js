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
    'ngRoute'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/overview.html',
        controller: 'OverviewCtrl',
        controllerAs: 'overview'
      })
      .when('/story/:id?', {
        templateUrl: 'views/client.html',
        controller: 'ClientCtrl',
        resolve: {
          client:function(ClientService,Client,$route){
            return $route.current.params.id ?
              ClientService.get($route.current.params.id) :
              new Client({});
          }}
      })
      .otherwise({
        redirectTo: '/'
      });
  });
