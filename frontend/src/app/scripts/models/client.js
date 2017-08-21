'use strict';

/**
 * @ngdoc service
 * @name smartiApp.Client
 * @description
 * # Client
 * Factory in the smartiApp.
 */
angular.module('smartiApp')
  .factory('Client', function ($http, $q, ENV) {

    function Client(data) {

      this.data = angular.merge({},data);

      this.save = function() {
        var deferred = $q.defer();

        $http.post(ENV.serviceBaseUrl + 'client/', this.data).then(function(data){
          deferred.resolve(new Client(data.data));
        }, function(data){
          deferred.reject(data.data);
        });

        return deferred.promise;
      };

      this.delete = function() {
        return $http.delete(ENV.serviceBaseUrl + 'client/' + this.data.id);
      };

    }

    return Client;
  });
