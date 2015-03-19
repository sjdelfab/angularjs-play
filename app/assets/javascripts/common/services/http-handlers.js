define([ 'angular' ], function(angular) {
	'use strict';

	var mod = angular.module('common.http-handler', []);
	mod.factory('ErrorHandler', function($location, userSessionContext) {
		return {
			handleError : function(status) {
				if (userSessionContext.isUserLoggedIn()) {
					if (status == 500) {
						$location.path('/error');
					} else if (status == 403 || status == 401) {
						$location.path('/sessionExpired');
					} else if (status == 404) {
						$location.path('/404');
					} else if (status == 400) {
						$location.path('/400');
					} else if (status == 408) {
						$location.path('/408');
					}
				}
			}
		};
	}).factory('httpErrorInterceptor', function($q, $location, UserService, ErrorHandler) {
		return {
			'requestError' : function(rejection) {
				ErrorHandler.handleError(rejection.status);
				return $q.reject(rejection);
			},
			'responseError' : function(rejection) {
				ErrorHandler.handleError(rejection.status);
				return $q.reject(rejection);
			}
		};
	});
	return mod;
});
