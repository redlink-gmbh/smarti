'use strict';

/**
 * @ngdoc service
 * @name smartiApp.ClientService
 * @description
 * # ClientService
 * Service in the smartiApp.
 */
angular.module('smartiApp')
  .service('ClientService', function ($http, $q, ENV, Configuration, Client) {

    this.list = function() {
      var deferred = $q.defer();

      $http.get(ENV.serviceBaseUrl + '/client').then(function(data){
        deferred.resolve(data.data.map(function(c){return new Client(c);}));
      });

      return deferred.promise;
    };

    this.getById = function(id,copy) {
      var deferred = $q.defer();

      $http.get(ENV.serviceBaseUrl + '/client/' + id).then(function(data){
        var newClient = new Client(data.data,copy);
        if (copy) {
          newClient.data.defaultClient = false;
        }
        deferred.resolve(newClient);
      });

      return deferred.promise;
    };

    this.loadAuthTokens = function (client) {
      return $http.get(ENV.serviceBaseUrl + '/client/' + client.data.id + '/token')
        .then(function (response) {
          return response.data;
        });
    };

    this.createAuthToken = function (client, label) {
      return $http
        .post(ENV.serviceBaseUrl + '/client/' + client.data.id + '/token', {
          label: label
        })
        .then(function (response) {
          return response.data;
        });
    };

    this.updateAuthToken = function (client, token) {
      return $http
        .put(ENV.serviceBaseUrl + '/client/' + client.data.id + '/token/' + token.id, token)
        .then(function (response) {
          return response.data;
        });
    };

    this.revokeAuthToken = function (client, token) {
      return $http
        .delete(ENV.serviceBaseUrl + '/client/' + client.data.id + '/token/' + token.id)
        .then(function (response) {
          return response.data;
        });
    };

    this.listUsers = function (client) {
      return $http
        .get(ENV.serviceBaseUrl + '/client/' + client.data.id + '/user')
        .then(function (response) {
          return response.data;
        });
    };

    this.createUser = function (user, client) {
      return $http
        .post(ENV.serviceBaseUrl + '/client/' + client.data.id + '/user', user)
        .then(function (response) {
          return response.data;
        });
    };

    this.addUser = function (login, client) {
      return $http
        .put(ENV.serviceBaseUrl + '/client/' + client.data.id + '/user/' + login, undefined)
        .then(function (response) {
          return response.data;
        });
    };

    this.removeUser = function (user, client) {
      return $http
        .delete(ENV.serviceBaseUrl + '/client/' + client.data.id + '/user/' + user.login)
        .then(function (response) {
          return response.data;
        });
    };

  });
