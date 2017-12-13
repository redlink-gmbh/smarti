'use strict';

/**
 * @ngdoc service
 * @name smartiApp.ConfigurationService
 * @description
 * # ConfigurationService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('ConfigurationService', function ($http, $q, ENV, Configuration) {

    this.getDefaultConfiguration = function() {
      var deferred = $q.defer();

      $http.get(ENV.serviceBaseUrl + '/config').then(function(data){
        deferred.resolve(new Configuration(data.data));
      });

      return deferred.promise;
    };

    this.getConfiguration = function(client) {
      var deferred = $q.defer();

      if(!client.data.id) {
        deferred.resolve(new Configuration());
      } else {
        $http.get(ENV.serviceBaseUrl + '/client/' + client.data.id + '/config').then(function(data){
          deferred.resolve(new Configuration(data.data));
        });
      }

      return deferred.promise;
    };

  });
