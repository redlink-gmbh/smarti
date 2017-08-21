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

      var self = this;

      this.innerData = {};

      this._data = {};

      angular.forEach(data, function(value,key){
        switch(key) {
          case '_class':
          case 'name':
          case 'displayName':
          case 'type':
          case 'enabled':
          case 'unbound': self.innerData[key] = angular.copy(value);
            break;
          default : self._data[key] = angular.copy(value);
        }
      });

      this.data = JSON.stringify(this._data,null,2);

      this.getDataAsObject = function() {
        var res = angular.merge(JSON.parse(this.data),this.innerData);
        res.name = null;
        return res;
      };

      this.clone = function(){
        return new ComponentConfiguration(this.getDataAsObject());
      }

    }

    return ComponentConfiguration;
  });
