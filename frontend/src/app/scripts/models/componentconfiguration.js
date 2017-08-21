'use strict';

/**
 * @ngdoc service
 * @name smartiApp.Client
 * @description
 * # Client
 * Factory in the smartiApp.
 */
angular.module('smartiApp')
  .factory('ComponentConfiguration', function () {

    function ComponentConfiguration(data) {

      this.data = data ? data : '{}'; //TODO serialize + deserialize

    }

    return ComponentConfiguration;
  });
