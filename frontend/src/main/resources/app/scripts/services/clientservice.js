'use strict';

/**
 * @ngdoc service
 * @name smartiApp.ClientService
 * @description
 * # ClientService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('ClientService', function ($http, $q, ENV) {

    this.list = function() {
      var deferred = $q.defer();

      $http.get(ENV + 'client', function(data){
        data.data = data.data.map(function(c){return new Client(c)});
        deferred.resolve(data);
      });

      return deferred.promise();
    };

    this.getById = function(id) {
      var deferred = $q.defer();

      $http.get(ENV + 'client/' + id, function(data){
        data.data = new Client(data.data);
        deferred.resolve(data);
      });

      return deferred.promise();
    }

  });
