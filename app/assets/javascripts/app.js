define(['angular', 'user', 'home'], function(angular) {
  'use strict';

  // We must already declare most dependencies here (except for common), or the submodules' routes
  // will not be resolved
  var app = angular.module('app', ['myapp.user','myapp.home','pascalprecht.translate']);
  app.config(['$translateProvider','$translatePartialLoaderProvider', function ($translateProvider, $translatePartialLoaderProvider) {      
      $translateProvider.useLoader('$translatePartialLoader', {
          urlTemplate: '/assets/i18n/{part}/{lang}.json'
       });
      $translateProvider.preferredLanguage('en');
  }]);
  return app;
});
