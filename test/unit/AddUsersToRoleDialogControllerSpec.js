define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap',
         'angular-ui-select','angular-sanitize','angular-translate','angular-translate-loader-partial',
         'angular-translate-loader-url'], 
        function(angular, mocks) {
    'use strict';

    describe("Add Users to Role Controller Test", function() {

        var createAddRolesController, $scope, $controller, userManagement, messageDialog, $modalInstance, $modal;
        beforeEach(function() {
            module('app');
            
            inject(function($rootScope, $injector, userManagement) {
                $scope = $rootScope.$new();
                $controller = $injector.get('$controller');
                $modalInstance = {};
                $modal = {};
                
                createAddRolesController = function() { return $controller('AddUsersToRoleDialogController', {
                      '$scope' : $scope,
                      '$modalInstance' : $modalInstance,                       
                      '$modal': $modal,
                      'userManagement' : userManagement,
                      'messageDialog': messageDialog,
                      'roleType': 'admin',
                      'roleName': 'roleName',
                      'usersNotInRole': {}
                    });
                };
            });
            
        });
        
        describe("Unique constraints violation", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    spyOn(userManagement,"addRoleMembers").andCallFake(function(selectedUsers,errorCallBack) {
                        var deferred = $q.defer();
                        var response = {};
                        response.data = {};
                        response.data.status = 'UNIQUE_CONSTRAINTS_VIOLATION';
                        deferred.resolve(response);
                        return deferred.promise;
                    });
                });
            });
            
            it('UNIQUE_CONSTRAINTS_VIOLATION', function() {
                var controller = createAddRolesController();
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Unable to add user(s) to role since user(s) already are members');
            });
        
        });


    });
});
