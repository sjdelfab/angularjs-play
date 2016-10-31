define(['angular', 'user', 'home'], function(angular) {
  'use strict';

  // We must already declare most dependencies here (except for common), or the submodules' routes
  // will not be resolved
  var app = angular.module('app', ['myapp.user','myapp.home','pascalprecht.translate']);
  app.config(['$translateProvider','$translatePartialLoaderProvider','$httpProvider', function ($translateProvider, $translatePartialLoaderProvider, $httpProvider) {      
      $translateProvider.useLoader('$translatePartialLoader', {
          urlTemplate: '/assets/i18n/{part}/{lang}.json'
       });
      $translateProvider.preferredLanguage('en');
      $translateProvider.useSanitizeValueStrategy('sanitize');
      
      $httpProvider.interceptors.push(function($q, $injector) {
          return {
            request: function(request) {
              // Add CSRF token for the Play CSRF filter
              var cookies = $injector.get('$cookies');
              var token = cookies.get('PLAY_CSRF_TOKEN');
              if (token) {
                // Play looks for a token with the name Csrf-Token
                // https://www.playframework.com/documentation/2.4.x/ScalaCsrf
                request.headers['Csrf-Token'] = token;
              }
              return request;
            }
          };
      });
  }]);
  return app;
});
