define(['angular', 'user', 'home'], function(angular) {
  'use strict';

  // We must already declare most dependencies here (except for common), or the submodules' routes
  // will not be resolved
  var app = angular.module('app', ['myapp.user','myapp.home','pascalprecht.translate','satellizer']);
  app.config(['$translateProvider','$translatePartialLoaderProvider','$httpProvider','$authProvider', function ($translateProvider, $translatePartialLoaderProvider, $httpProvider, $authProvider) {      
      $translateProvider.useLoader('$translatePartialLoader', {
          urlTemplate: '/assets/i18n/{part}/{lang}.json'
       });
      $translateProvider.preferredLanguage('en');
      $translateProvider.useSanitizeValueStrategy('sanitize');
      
      $httpProvider.interceptors.push(function($q, $injector) {
          return {
            request: function(request) {
              var $auth = $injector.get('$auth');
              if ($auth.isAuthenticated()) {
                  request.headers['X-Auth-Token'] = $auth.getToken();
              }  
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
      
      $authProvider.httpInterceptor = true; // Add Authorization header to HTTP request
      $authProvider.tokenName = 'token';
      $authProvider.tokenPrefix = 'satellizer'; // Local Storage name prefix
      $authProvider.authHeader = 'X-Auth-Token';
      $authProvider.platform = 'browser';
      $authProvider.storage = 'localStorage';
      
      $authProvider.loginRedirect = '/#/login';
      $authProvider.logoutRedirect = '/';
      $authProvider.loginUrl = '/login';
      $authProvider.loginRoute = '/login';

  }]);
  return app;
});
