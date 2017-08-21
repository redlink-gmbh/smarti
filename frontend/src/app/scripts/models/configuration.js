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

      var self = this;

      this.data = {
        "queryBuilder": (data && data.queryBuilder) ? data.queryBuilder.map(function(d){return new ComponentConfiguration(d)}) : []
      };

      this.addComponent = function(type,component) {
        this.data[type].push(component)
      };

      function getJsonData() {
        return {
          queryBuilder: self.data.queryBuilder.map(function(c){return c.getDataAsObject()})
        }
      }

      this.save = function(client) {
        return $http.post(ENV.serviceBaseUrl + 'client/' + client.data.id + '/config', getJsonData());
      };

      this.remove = function(type,item) {
        var index = this.data[type].indexOf(item);
        this.data[type].splice(index, 1);
      };

    }

    return Configuration;
  });
