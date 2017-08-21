'use strict';

/**
 * @ngdoc service
 * @name smartiApp.Client
 * @description
 * # Client
 * Factory in the smartiApp.
 */
angular.module('smartiApp')
  .factory('Configuration', function ($http, ENV, ComponentConfiguration) {

    function Configuration(data) {

      this.data = {
        "queryBuilders": (data && data.queryBuilders) ? data.queryBuilders.map(function(d){return new ComponentConfiguration(d)}) : '{}'
      };

      this.save = function(client) {
        return $http.post(ENV.serviceBaseUrl + 'client/' + client.data.id + '/config', this.data);
      };

    }

    return Configuration;
  });
