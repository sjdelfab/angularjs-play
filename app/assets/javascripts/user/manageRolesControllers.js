/**
 * User controllers.
 */
define([ 'angular' ], function(angular) {
	'use strict';

	var UserRolesController = function($scope, $translate, $translatePartialLoader) { 
	    
	    if (!$translatePartialLoader.isPartAvailable('user')) {
            $translatePartialLoader.addPart('user');
            $translate.refresh();
        }
	};
	
	UserRolesController.$inject = ['$scope','$translate','$translatePartialLoader'];
		
	var EditUserRoleController = function($scope, $location, $routeParams, $modal, blockUI, 
	                                      userManagement, $translate, $translatePartialLoader) { 
	    
	    $scope.error = {};
        $scope.error.message = '';
        $scope.error.displayErrorMessage = false;
	    
        var admin_role_name = "Administrator";
        var resource_manager_role_name = "Resource Manager";
        var error_loading_roles = "Error obtaining role members";
        var error_loading_users = "Error obtaining users";
        var deleting_role = "Error deleting role member";
        
        var loadi18n = function() {
            $translate(['admin_role_name', 'resource_manager_role_name',
                        'user_roles.messages.error_loading_roles',
                        'user_roles.messages.error_loading_users',
                        'user_roles.messages.deleting_role']).then(function (translations) {
                admin_role_name = translations.admin_role_name;
                resource_manager_role_name = translations.resource_manager_role_name;
                error_loading_roles = translations['user_roles.messages.error_loading_roles'];
                error_loading_users = translations['user_roles.messages.error_loading_users'];
                deleting_role = translations['user_roles.messages.deleting_role'];
            });
        };
        
        if (!$translatePartialLoader.isPartAvailable('user')) {
            $translatePartialLoader.addPart('user');
            $translate.refresh().then(function() {
                loadi18n();
            });
        } else {
            loadi18n();
        }
        
	    var roleType;
	    if ($routeParams.role === 'admin') {
	        roleType = 'admin';
	    } else if ($routeParams.role === 'resource_manager') {
	        roleType = 'resource_manager';
	    } else {
	        $location.path('/userroles');
	    }

	    var roleTypeToRoleName = function(roleType) {
            if (roleType === 'admin') {
                return admin_role_name;
            } else if (roleType === 'resource_manager') {
                return resource_manager_role_name;
            } else {
                return undefined;
            }   
        };
	    
	    if (angular.isDefined(roleType)) {
	        $scope.roleName = roleTypeToRoleName(roleType);
	    }
	    
	    $scope.users = {};
	    
	    var showErrorMessage = function(message) {
            clearMessages();
            $scope.error.displayErrorMessage = true;
            $scope.error.message = message;
        };

        var clearMessages = function() {
            $scope.error.message = '';
            $scope.error.displayErrorMessage = false;
        };
	    
	    var loadRoleMembers = function() {
            blockUI.start();
            var errorCallback = function() {
                showErrorMessage(error_loading_roles);
            };
            userManagement.getRoleMembers(roleType, errorCallback).then(function(data) {
                $scope.users = data;
            })['finally'](function() {
                blockUI.stop();
            });
        };
        
        $scope.deleteUser = function(user) {
            blockUI.start();
            var errorCallback = function() {
                showErrorMessage(deleting_role);
            };
            userManagement.deleteRoleMember(user.userId,roleType, errorCallback).then(function(data) {
                loadRoleMembers();
            })['finally'](function() {
                blockUI.stop();
            });
        };
        
        var getUsersNotInRole = function() {
            blockUI.start();
            var errorCallback = function() {
                showErrorMessage(error_loading_users);
            };
            var getRoleNonMembersPromise = userManagement.getRoleNonMembers(roleType, errorCallback);
            getRoleNonMembersPromise['finally'](function() {
                blockUI.stop();
            });
            return getRoleNonMembersPromise;
        };
        
        $scope.addUsers = function() {
            getUsersNotInRole().then(function(usersNotInRole) {
                var modalInstance = $modal.open({
                    backdrop : 'static',
                    templateUrl : '/assets/javascripts/user/addUsersToRoleDialog.html',
                    controller : 'AddUsersToRoleDialogController',
                    scope: $scope,
                    resolve : {
                        roleType : function() {
                            return roleType;
                        },
                        roleName : function() {
                            return $scope.roleName;
                        },
                        usersNotInRole : function() {
                            return usersNotInRole;
                        }
                    }
                });
                modalInstance.result.then(function() {
                    loadRoleMembers();
                });
            });                        
        };
        
        loadRoleMembers();
	    
	};
	
    EditUserRoleController.$inject = [ '$scope', '$location', '$routeParams', '$modal', 'blockUI', 
                                       'userManagement','$translate', '$translatePartialLoader'];
	
    // ****** Start Add Group Users Controller ******
    
    var AddUsersToRoleDialogController = ['$scope', '$modalInstance', '$modal', 'userManagement', 'messageDialog', 'roleType', 
                                          'roleName', 'usersNotInRole', '$translate', '$translatePartialLoader', function($scope, $modalInstance, $modal, userManagement, 
                                           messageDialog, roleType, roleName, usersNotInRole, $translate, $translatePartialLoader) {

        // For testing, set to defaults
        var please_select_user = "Please select one or more users";
        var error_adding_users = "Error adding user(s)";
        var add_user_unique_constraints_violation = "Unable to add user(s) to role since user(s) already are members";
        
        var loadi18n = function() { 
            $translate(['add_users_to_role_dialog.please_select_user',
                        'add_users_to_role_dialog.error_adding_users',
                        'add_users_to_role_dialog.add_user_unique_constraints_violation']).then(function (translations) {
                            please_select_user = translations['add_users_to_role_dialog.please_select_user'];
                            error_adding_users = translations['add_users_to_role_dialog.error_adding_users'];
                            add_user_unique_constraints_violation = translations['add_users_to_role_dialog.add_user_unique_constraints_violation'];
            });
        };
        
        if (!$translatePartialLoader.isPartAvailable('user')) {
            $translatePartialLoader.addPart('user');
            $translate.refresh().then(function() {
                loadi18n();
            });
        } else {
            loadi18n();
        }
        
        $scope.error = {};
        
        $scope.roleName = roleName;
        $scope.saveInProgress = false;
        $scope.roleUsers = {};
        $scope.roleUsers.availableUsers =  usersNotInRole;
        $scope.roleUsers.selectedUsers = {};

        $scope.close = function() {
            $modalInstance.close(undefined);
        };
      
        $scope.ok = function() {
            clearErrorMessage();
            if ($scope.roleUsers.selectedUsers.length === 0) {
                showErrorMessage(please_select_user);
                return;
            }
            $scope.saveInProgress = true;
            var errorCallback = function() {
                showErrorMessage(error_adding_users);
            };
            userManagement.addRoleMembers($scope.roleUsers.selectedUsers, errorCallback).then(function(response) {
                if (response.data.status === 'UNIQUE_CONSTRAINTS_VIOLATION') {
                    showErrorMessage(add_user_unique_constraints_violation);
                } else {
                    $scope.close();
                }
            })['finally'](function() {
                $scope.saveInProgress = false;
            });
        };

        var showErrorMessage = function(msg) {
            clearErrorMessage();
            $scope.error.message = msg;
            $scope.error.displayErrorMessage = true;
        };

        var clearErrorMessage = function() {
            $scope.error.message = '';
            $scope.error.displayErrorMessage = false;
        };

        clearErrorMessage();

    }];
    
	return {
		UserRolesController : UserRolesController,
		EditUserRoleController: EditUserRoleController,
		AddUsersToRoleDialogController: AddUsersToRoleDialogController,
	};

});
