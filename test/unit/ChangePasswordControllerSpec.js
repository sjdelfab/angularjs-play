define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Change password Controller Test", function() {

        var createChangePasswordController, $scope, $controller, userManagement, messageDialog, $modalInstance, $modal, user;
        beforeEach(function() {
            module('app');
            
            inject(function($rootScope, $injector, userManagement) {
                $scope = $rootScope.$new();
                $controller = $injector.get('$controller');
                $modalInstance = {};
                $modal = {};
                user = {};
                user.id = '1234';
                
                createChangePasswordController = function() { return $controller('ChangePasswordController', {
                      '$scope' : $scope,
                      '$modalInstance' : $modalInstance,                       
                      '$modal': $modal,
                      'messageDialog': messageDialog,
                      'user': user
                    });
                };
            });
            
        });
        
        describe("Invalid Password Error", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"changeUserPassword").andCallFake(function(userId,password,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'INVALID_PASSWORD';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                });
            });
            
            it('INVALID_PASSWORD', function() {
                var controller = createChangePasswordController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Only ASCII characters allowed');
            });
        
        });


        describe("Password Not Strong Enough", function() {
            
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"changeUserPassword").andCallFake(function(userId,invalidPassword,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'PASSWORD_NOT_STRONG_ENOUGH';
                        response.data.message = 'Password not strong enough';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                });
            });
            
            it('PASSWORD_NOT_STRONG_ENOUGH', function() {
                var controller = createChangePasswordController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Password not strong enough');
            });
        
        });
        
        
        describe("Validation", function() {
            
            it('Passwords do not match', function() {
                var controller = createChangePasswordController();
                $scope.passwords.password = 'password1';
                $scope.passwords.retypePassword = 'password2';
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Passwords do not match');
            });
        
        });
    });
});
