define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Change password Controller Test", function() {

        
        
        var createChangePasswordController, $scope, $controller, userManagement, messageDialog, $uibModalInstance, $uibModal, user;
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
                
                createChangePasswordController = function() { return $controller('ChangePasswordController', {
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
                    spyOn(userManagement,"changeUserPassword").and.returnValue(changeMyPasswordDeferred.promise);
                });
            });
            
            it('INVALID_PASSWORD', function() {
                var controller = createChangePasswordController();
                
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
                var controller = createChangePasswordController();
                
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
