define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap'], 
		function(angular, mocks) {
	'use strict';

	describe("User Session Context Failed Request Login Test", function() {

		var testuserSessionContext, $scope, $qpromise, mockPlayRoutes, errorMessage = 'Server error';
		beforeEach(function() {
            module('app',function($provide) {
            	mockPlayRoutes = {
            	    'controllers': {
            	    	'Application': {
            	    		login: function() {
            	    			var postFunction = {};
            	    			postFunction.post = function(credentials) {
            	    				var deferred = $qpromise.defer();
            						deferred.reject(errorMessage);
            						var promise = deferred.promise; 
            						promise.error = function(errorCallback) {
            							this.then(null,errorCallback);
            						}
            						return promise;
            	    			}
            	    			return postFunction;
            	    		}
            	    	}
            		}
            	};
            	
            	$provide.value('playRoutes', mockPlayRoutes);
            });
            
			inject(function($rootScope, $injector, userSessionContext, $q) {
				$scope = $rootScope.$new();
				testuserSessionContext = userSessionContext;
				$qpromise = $q;
			});
		});

		it('Server error', function() {
			var credentials = {};
			credentials.email = 'test@email.com';
			credentials.password = 'password';
			var failedLogin = false;
			var failedReason = '';
			testuserSessionContext.loginUser(credentials, function(reason) {
				failedLogin = true;
				failedReason = reason;
			});
			$scope.$apply();
			expect(failedLogin).toBe(true);
			expect(failedReason).toBe('Server error');
		});
		
		describe("User Service - Invalid username/password", function() {
			beforeEach(function() {
				errorMessage = {};
				errorMessage.login_error_status = 'INVALID_USERNAME_PASSWORD';
			});
			
			it('Invalid username/password', function() {
				var credentials = {};
				credentials.email = 'test@email.com';
				credentials.password = 'password';
				var failedLogin = false;
				var failedReason = '';
				testuserSessionContext.loginUser(credentials, function(reason) {
					failedLogin = true;
					failedReason = reason.login_error_status;
				});
				$scope.$apply();
				expect(failedLogin).toBe(true);
				expect(failedReason).toBe('INVALID_USERNAME_PASSWORD');
			});
		});
		
		describe("User Service - Account locked", function() {
			beforeEach(function() {
				errorMessage = {};
				errorMessage.login_error_status = 'ACCOUNT_LOCKED';
			});
			
			it('Account locked', function() {
				var credentials = {};
				credentials.email = 'test@email.com';
				credentials.password = 'password';
				var failedLogin = false;
				var failedReason = '';
				testuserSessionContext.loginUser(credentials, function(reason) {
					failedLogin = true;
					failedReason = reason.login_error_status;
				});
				$scope.$apply();
				expect(failedLogin).toBe(true);
				expect(failedReason).toBe('ACCOUNT_LOCKED');
			});
		});
		
	});
	
   	
});
