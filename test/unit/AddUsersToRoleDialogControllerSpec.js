define([ 'angular', 'angularMocks','app','user','common','angular-cookies', 'angular-route','ui-bootstrap',
         'angular-ui-select','angular-sanitize','angular-translate','angular-translate-loader-partial',
         'angular-translate-loader-url'], 
        function(angular, mocks) {
    'use strict';

    describe("Add Users to Role Controller Test", function() {

        
        
        var createAddRolesController, $scope, $controller, userManagement, messageDialog, $uibModalInstance, $uibModal;
        beforeEach(function() {
            module('app',function($provide) {
                $provide.value('playRoutes', mockPlayRoutes);
               }
            );
            
            inject(function($rootScope, $injector, userManagement) {
                $scope = $rootScope.$new();
                $controller = $injector.get('$controller');
                $uibModalInstance = {};
                $uibModal = {};
                
                createAddRolesController = function() { return $controller('AddUsersToRoleDialogController', {
                      '$scope' : $scope,
                      '$uibModalInstance' : $uibModalInstance,                       
                      '$uibModal': $uibModal,
                      'userManagement' : userManagement,
                      'messageDialog': messageDialog,
                      'roleType': 'admin',
                      'roleName': 'roleName',
                      'usersNotInRole': {}
                    });
                };
            });
            
        });
        
        var addRoleMembersDeferred;
        
        describe("Unique constraints violation", function() {
        
            beforeEach(function() {
                inject(function($rootScope, $injector, userManagement, $q) {
                    addRoleMembersDeferred = $q.defer();
                    spyOn(userManagement,"addRoleMembers").and.returnValue(addRoleMembersDeferred.promise);
                });
            });
            
            it('UNIQUE_CONSTRAINTS_VIOLATION', function() {
                var controller = createAddRolesController();
                
                var response = {};
                response.data = {};
                response.data.status = 'UNIQUE_CONSTRAINTS_VIOLATION';
                
                addRoleMembersDeferred.resolve(response);
                
                $scope.ok();
                $scope.$apply();
                expect($scope.error.displayErrorMessage).toBe(true);
                expect($scope.error.message).toBe('Unable to add user(s) to role since user(s) already are members');
            });
        
        });


    });
});
