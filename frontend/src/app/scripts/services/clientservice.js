'use strict';

/**
 * @ngdoc service
 * @name smartiApp.ClientService
 * @description
 * # ClientService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('ClientService', function ($http, $q, ENV, Configuration, Client) {

    this.list = function() {
      var deferred = $q.defer();console.log(ENV)

      $http.get(ENV.serviceBaseUrl + 'client').then(function(data){
        deferred.resolve(data.data.map(function(c){return new Client(c);}));
      });

      return deferred.promise;
    };

    this.getById = function(id) {
      var deferred = $q.defer();

      $http.get(ENV.serviceBaseUrl + 'client/' + id).then(function(data){
        deferred.resolve(new Client(data.data));
      });

      return deferred.promise;
    };

  });
