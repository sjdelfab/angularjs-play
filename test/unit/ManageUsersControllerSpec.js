define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Manage Users Controller Test", function() {

        var createManageUsersController, $scope, $controller, $location, userManagement, $routeParams, blockUI, $modal, messageDialog;
        
        beforeEach(function() {
            module('app',function($provide) {
                $provide.value('playRoutes', mockPlayRoutes);
                $provide.value('$auth',mockAuth);
               }
            );
            
            inject(function($rootScope, $injector, userManagement, $q) {
                $scope = $rootScope.$new();
                $scope.userForm = {};
                $scope.userForm.validateForm = function() { return true; };
                $controller = $injector.get('$controller');
                $location = $injector.get('$location');
                $routeParams = {};
                blockUI = {};
                blockUI.start = function(msg) {};
                blockUI.stop = function() {};
                $modal = {};
                messageDialog = {};
                messageDialog.showConfirmationMessage = function(modal,confirmationCallback,title,message) {
                    confirmationCallback();
                }
                
                spyOn(userManagement,"getUsers").and.callFake(function(errorCallBack) {
                    var deferred = $q.defer();
                    var data = new Array(); 
                    var user = {};
                    user.name = 'Simon';
                    user.email = 'simon@email.com';
                    user.id = 1;
                    data.push(user);
                    deferred.resolve(data);
                    return deferred.promise;
                });
                
                createManageUsersController = function() { return $controller('UsersController', {
                      '$scope' : $scope,
                      '$location' : $location,                       
                       '$routeParams': $routeParams,
                       'blockUI': blockUI,
                       'userManagement' : userManagement,
                       'messageDialog': messageDialog
                    });
                };
            });
            
        });
        
        describe("Delete User", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"deleteUser").and.callFake(function(userId,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'FK_CONSTRAINTS_VIOLATION';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                    
                });
            });
            
            it('FK_CONSTRAINTS_VIOLATION', function() {
                var controller = createManageUsersController();
                
                var user = {};
                user.name = 'Simon';
                user.id = 1;
                user.email = 'simon@email.com';
                $scope.deleteUser(user);
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('User cannot be deleted because it is being used. Disable instead');
            });
        
        });

    });
});
