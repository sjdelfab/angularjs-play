define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route',
         'ui-bootstrap','angular-translate','angular-translate-loader-partial','angular-translate-loader-url'], 
		function(angular, mocks) {
	'use strict';
	
	describe("Login Controller Test", function() {

		var createLoginController, $scope, $controller, $location, userSessionContext, loginErrorStatus = 'Server error';
		beforeEach(function() {
		    module('app',function($provide) {
                $provide.value('playRoutes', mockPlayRoutes);
               }
            );
			
			inject(function($rootScope, $injector, userSessionContext, $q) {
				$scope = $rootScope.$new();
				$controller = $injector.get('$controller');
				$location = $injector.get('$location');
				
				spyOn(userSessionContext,"loginUser").and.callFake(function(credentials,errorCallBack) {
                    var deferred = $q.defer();
                    deferred.resolve('OK');
                    errorCallBack(loginErrorStatus)
                    return deferred.promise;
                });
				
				createLoginController = function() { return $controller('LoginController', {
					  '$scope' : $scope,
					  '$location' : $location,
					   'userSessionContext' : userSessionContext
				    });
				};
			});
		});

		it('Login no username or password', function() {
			var controller = createLoginController();
			$scope.login({});
			expect($scope.error.displayErrorMessage).toBe(true);
			expect($scope.error.loginErrorMessage).toBe('Please provide an email and password');
		});

		it('Server error', function() {
			var controller = createLoginController();
			
			var credentials = {};
			credentials.email = 'test@email.com';
			credentials.password = 'password';
			$scope.login(credentials);
			$scope.$apply();
			expect($scope.error.displayErrorMessage).toBe(true);
			expect($scope.error.loginErrorMessage).toBe('Unable to login');
		});
		
		describe("Login Controller - Invalid username or password", function() {
			beforeEach(function() {
				loginErrorStatus = {};
				loginErrorStatus.login_error_status = "INVALID_USERNAME_PASSWORD";
			});
			
			it('Invalid username or password', function() {
				var controller = createLoginController();
				var credentials = {};
				credentials.email = 'test@email.com';
				credentials.password = 'password';
				$scope.login(credentials);
				$scope.$apply();
				expect($scope.error.displayErrorMessage).toBe(true);
				expect($scope.error.loginErrorMessage).toBe('Invalid username or password');
			});
		});
		
		describe("Login Controller - Account locked", function() {
			beforeEach(function() {
				loginErrorStatus = {};
				loginErrorStatus.login_error_status = "ACCOUNT_LOCKED";
			});
			
			it('Account locked', function() {
				var controller = createLoginController();
				var credentials = {};
				credentials.email = 'test@email.com';
				credentials.password = 'password';
				$scope.login(credentials);
				$scope.$apply();
				expect($scope.error.displayErrorMessage).toBe(true);
				expect($scope.error.loginErrorMessage).toBe('Account Locked');
			});
		});
	});
});
