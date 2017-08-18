'use strict';

/**
 * @ngdoc service
 * @name smartiApp.Client
 * @description
 * # Client
 * Factory in the smartiApp.
 */
angular.module('smartiApp')
  .factory('Configuration', function ($http, ENV) {

    function Configuration(data) {

      this.data = data ? JSON.stringify(data,null,2) : '{}';

      this.save = function(client) {
        return $http.post(ENV.serviceBaseUrl + 'client/' + client.data.id + '/config', this.data);
      };

    }

    return Configuration;
  });
