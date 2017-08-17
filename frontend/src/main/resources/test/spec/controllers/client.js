'use strict';

describe('Controller: ClientCtrl', function () {

  // load the controller's module
  beforeEach(module('smartiApp'));

  var ClientCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ClientCtrl = $controller('ClientCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(ClientCtrl.awesomeThings.length).toBe(3);
  });
});
