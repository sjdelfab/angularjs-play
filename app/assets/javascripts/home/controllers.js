define([], function() {
	'use strict';

	/** Controls the index page */
	var HomeController = function($scope, $rootScope, $location) {
		$rootScope.pageTitle = 'Welcome';
		
	};
	HomeController.$inject = [ '$scope', '$rootScope', '$location'];

	/** Controls the header */
	var HeaderController = function($scope, userSessionContext, $location) {
		// Wrap the current user from the service in a watch expression
		$scope.$watch(function() {
			var user = userSessionContext.getLoggedInUser();
			return user;
		}, function(user) {
			$scope.user = user;
		}, true);

		$scope.logout = function() {
		    userSessionContext.logout();
			$scope.user = undefined;
			$location.path('/');
		};
	};
	HeaderController.$inject = [ '$scope', 'userSessionContext', '$location' ];

	return {
		HeaderController : HeaderController,
		HomeController : HomeController
	};

});
