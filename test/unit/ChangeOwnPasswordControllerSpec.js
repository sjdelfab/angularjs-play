define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Change my password Controller Test", function() {

        var createChangeMyPasswordController, $scope, $controller, userManagement, messageDialog, $modalInstance, $modal, user;
        beforeEach(function() {
            module('app');
            
            inject(function($rootScope, $injector, userManagement) {
                $scope = $rootScope.$new();
                $controller = $injector.get('$controller');
                $modalInstance = {};
                $modal = {};
                user = {};
                user.id = '1234';
                
                createChangeMyPasswordController = function() { return $controller('ChangeOwnPasswordController', {
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
                    spyOn(userManagement,"changeMyPassword").andCallFake(function(currentPassword,password,errorCallBack) {
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
                var controller = createChangeMyPasswordController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Only ASCII characters allowed');
            });
        
        });


        describe("Password Not Strong Enough", function() {
            
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"changeMyPassword").andCallFake(function(currentPassword,invalidPassword,errorCallBack) {
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
                var controller = createChangeMyPasswordController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Password not strong enough');
            });
        
        });
        
       describe("Current password is wrong", function() {
            
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"changeMyPassword").andCallFake(function(currentPassword,invalidPassword,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'INVALID_CURRENT_PASSWORD';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                });
            });
            
            it('INVALID_CURRENT_PASSWORD', function() {
                var controller = createChangeMyPasswordController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Incorrect current password');
            });
        
        });
                
        describe("Validation", function() {
            
            it('Passwords do not match', function() {
                var controller = createChangeMyPasswordController();
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
