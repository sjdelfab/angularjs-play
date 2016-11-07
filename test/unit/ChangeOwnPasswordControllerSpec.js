define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Change my password Controller Test", function() {

       
        
        var createChangeMyPasswordController, $scope, $controller, userManagement, messageDialog, $uibModalInstance, $uibModal, user;
        beforeEach(function() {
            module('app',function($provide) {
                $provide.value('playRoutes', mockPlayRoutes);
                $provide.value('$auth',mockAuth);
               }
            );
            
            inject(function($rootScope, $injector, userManagement) {
                $scope = $rootScope.$new();
                $controller = $injector.get('$controller');
                $uibModalInstance = {};
                $uibModal = {};
                user = {};
                user.id = '1234';
                
                createChangeMyPasswordController = function() { return $controller('ChangeOwnPasswordController', {
                      '$scope' : $scope,
                      '$uibModalInstance' : $uibModalInstance,                       
                      '$uibModal': $uibModal,
                      'messageDialog': messageDialog,
                      'user': user
                    });
                };
            });
            
        });
        
        var changeMyPasswordDeferred;
        
        describe("Invalid Password Error", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    changeMyPasswordDeferred = $q.defer();
                    spyOn(userManagement,"changeMyPassword").and.returnValue(changeMyPasswordDeferred.promise);
                });
            });
            
            it('INVALID_PASSWORD', function() {
                var controller = createChangeMyPasswordController();
                
                var response = {};
                response.data = {};
                response.data.status = 'INVALID_PASSWORD';
                changeMyPasswordDeferred.resolve(response);
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Only ASCII characters allowed');
            });
        
            it('PASSWORD_NOT_STRONG_ENOUGH', function() {
                var controller = createChangeMyPasswordController();
                
                var response = {};
                response.data = {};
                response.data.status = 'PASSWORD_NOT_STRONG_ENOUGH';
                response.data.message = 'Password not strong enough';
                changeMyPasswordDeferred.resolve(response);
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Password not strong enough');
            });
            
            it('INVALID_CURRENT_PASSWORD', function() {
                var controller = createChangeMyPasswordController();
                
                var response = {};
                response.data = {};
                response.data.status = 'INVALID_CURRENT_PASSWORD';
                changeMyPasswordDeferred.resolve(response);
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Incorrect current password');
            });
            
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
