/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

'use strict';

angular.module('smartiApp')
  .directive('usernameExists', ['UserService', '$q', function (UserService, $q) {
    return {
      restrict: 'A',
      require: 'ngModel',
      link: function($scope, $element, $attrs, ngModel) {
            var shouldExist = $scope.$eval($attrs.usernameExists);
        ngModel.$asyncValidators.usernameExists = function(modelValue, viewValue) {
          var value = modelValue || viewValue;

          return UserService.checkUsernameExists(value)
            .then(function(exists) {
              if (exists !== shouldExist) return $q.reject(exists);
              return exists;
            });
        };
      }
    };
  }]);
