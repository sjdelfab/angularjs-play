define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap','angular-ui-select','angular-sanitize'], 
        function(angular, mocks) {
    'use strict';

    describe("Edit Controller Test", function() {

        var createEditUserController, $scope, $controller, $location, userManagement, $routeParams, blockUI;
        beforeEach(function() {
            module('app');
            
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
                
                createEditUserController = function() { return $controller('EditUserController', {
                      '$scope' : $scope,
                      '$location' : $location,                       
                       '$routeParams': $routeParams,
                       'blockUI': blockUI,
                       'userManagement' : userManagement
                    });
                };
            });
            
        });
        
        describe("Update User", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"updateUser").andCallFake(function(user,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'UNIQUE_CONSTRAINTS_VIOLATION';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                    spyOn(userManagement,"getUser").andCallFake(function(externalisedUserId,errorCallBack) {
                        var deferred = $q.defer();
                        var data = {}; 
                        var user = {};
                        user.name = 'Simon';
                        user.email = 'simon@email.com';
                        data.user = user;
                        data.groups = {};
                        deferred.resolve(data);
                        return deferred.promise;
                    });
                });
            });
            
            it('UNIQUE_CONSTRAINTS_VIOLATION', function() {
                var controller = createEditUserController();
                
                $scope.save();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Email address must be unique');
            });
        
        });


        describe("Create User", function() {
            
            function setupCreateUser(response) {
                return function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"createUser").andCallFake(function(user,errorCallBack) {
                        var deferred = $q.defer();
                        deferred.resolve(response);
                        return deferred.promise;
                    });                    
                }
            }
            
            describe("UNIQUE_CONSTRAINTS_VIOLATION", function() {
                beforeEach(function() {
                    $routeParams.id = 'new';
                    var response = {};
                    response.data = {};
                    response.data.status = 'UNIQUE_CONSTRAINTS_VIOLATION';
                    inject(setupCreateUser(response));
                });
                
                it('UNIQUE_CONSTRAINTS_VIOLATION', function() {
                    var controller = createEditUserController();
                    $scope.save();
                    $scope.$apply();
                    expect($scope.error.displayErrorMessage).toBe(true);
                    expect($scope.error.message).toBe('Email address must be unique');
                });
            });
            
            describe("INVALID_PASSWORD", function() {
                beforeEach(function() {
                    $routeParams.id = 'new';
                    var response = {};
                    response.data = {};
                    response.data.status = 'INVALID_PASSWORD';
                    inject(setupCreateUser(response));
                });
                
                it('INVALID_PASSWORD', function() {
                    var controller = createEditUserController();
                    $scope.save();
                    $scope.$apply();
                    expect($scope.error.displayErrorMessage).toBe(true);
                    expect($scope.error.message).toBe('Invalid password');
                });
            });
        
            describe("PASSWORD_NOT_STRONG_ENOUGH", function() {
                beforeEach(function() {
                    $routeParams.id = 'new';
                    var response = {};
                    response.data = {};
                    response.data.status = 'PASSWORD_NOT_STRONG_ENOUGH';
                    response.data.message = 'Password not strong enough';
                    inject(setupCreateUser(response));
                });
                
                it('PASSWORD_NOT_STRONG_ENOUGH', function() {
                    var controller = createEditUserController();
                    $scope.save();
                    $scope.$apply();
                    expect($scope.error.displayErrorMessage).toBe(true);
                    expect($scope.error.message).toBe('Password not strong enough');
                });
            });
            
        });
    });
});
