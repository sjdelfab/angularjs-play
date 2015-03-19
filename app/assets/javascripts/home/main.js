define(['angular', './routes', './controllers'], function(angular, routes, controllers) {
  'use strict';

  var mod = angular.module('myapp.home', ['ngRoute', 'home.routes','user.services']);
  mod.controller('HeaderController', controllers.HeaderController);
  return mod;
});
