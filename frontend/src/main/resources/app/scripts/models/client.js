'use strict';

/**
 * @ngdoc service
 * @name smartiApp.Client
 * @description
 * # Client
 * Factory in the smartiApp.
 */
angular.module('smartiApp')
  .factory('Client', function ($http, ENV) {

    function Client(data) {

      this.data = angular.merge({},data);

      this.save = function() {
         return $http.post(ENV + 'client', this.data);
      };

      this.delete = function() {
        return $http.delete(ENV + 'client', this.data);
      };

    }

    return Client;
  });
