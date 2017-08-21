'use strict';

describe('Directive: json', function () {

  // load the directive's module
  beforeEach(module('smartiApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<json></json>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the json directive');
  }));
});
