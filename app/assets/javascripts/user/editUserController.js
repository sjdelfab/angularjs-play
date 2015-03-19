define([ 'angular' ], function(angular) {
    'use strict';
   
    var EditUserController = function($scope, $location, $routeParams, blockUI, $modal, 
                                      userManagement,$translate, $translatePartialLoader) {

        $scope.error = {};
        $scope.error.message = '';
        $scope.error.displayErrorMessage = false;

        $scope.status = {};
        $scope.status.message = 'Successfully updated user';
        $scope.status.displayMessage = false;

        $scope.page = $routeParams.page;
        
        $scope.passwords = {};
        $scope.passwords.password = undefined;
        $scope.passwords.retypePassword = undefined;

        $scope.roles = {};
        
        var saving_progress_message = "Saving...";
        var admin_role_name = "Administrator";
        var resource_manager_role_name = "Resource Manager";
        var new_user_title = "New User";
        var error_loading = "Error obtaining user";
        var error_updating = "Error updating user";
        var unique_email = "Email address must be unique";
        var invalid_password = "Invalid password";
        var deleting_role = "Error deleting role membership";
        
        var loadi18n = function() {
            $translate(['saving_progress_message', 'admin_role_name', 'resource_manager_role_name',
                        'edit_user.new_user_title',
                        'edit_user.messages.successful_update',
                        'edit_user.messages.error_loading',
                        'edit_user.messages.error_updating',
                        'edit_user.messages.unique_email',
                        'edit_user.messages.invalid_password',
                        'edit_user.messages.delete_role']).then(function (translations) {
                saving_progress_message = translations.saving_progress_message;
                admin_role_name = translations.admin_role_name;
                resource_manager_role_name = translations.resource_manager_role_name;
                new_user_title = translations['edit_user.new_user_title'];
                $scope.status.message = translations['edit_user.messages.successful_update'];
                error_loading = translations['edit_user.messages.error_loading'];
                error_updating = translations['edit_user.messages.error_updating'];
                unique_email = translations['edit_user.messages.unique_email'];
                invalid_password = translations['edit_user.messages.invalid_password'];
                deleting_role = translations['edit_user.messages.delete_role'];
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
        
        var loadUser = function(externalisedUserId) {
            var errorCallback = function() {
                showErrorMessage(error_loading);
            };
            blockUI.start();
            userManagement.getUser(externalisedUserId, errorCallback).then(function(data) {
                $scope.user = data.user;
                $scope.roles = data.roles;
                $scope.editUserTitle = data.user.name;
            })['finally'](function() {
                blockUI.stop();
            });
        };

        var showErrorMessage = function(message) {
            clearMessages();
            $scope.error.displayErrorMessage = true;
            $scope.error.message = message;
        };

        var showUpdatedMessage = function() {
            clearMessages();
            $scope.status.displayMessage = true;
        };

        var clearMessages = function() {
            $scope.error.message = '';
            $scope.error.displayErrorMessage = false;
            $scope.status.displayMessage = false;
        };

        $scope.save = function() {
            if ($scope.userForm.validateForm()) {
                var errorCallback = function() {
                    showErrorMessage(error_updating);
                };
                blockUI.start(saving_progress_message);
                if ($routeParams.id !== 'new') {
                    userManagement.updateUser($scope.user, errorCallback).then(function(response) {
                        if (response.data.status === 'UNIQUE_CONSTRAINTS_VIOLATION') {
                            showErrorMessage(unique_email);
                        } else {
                            showUpdatedMessage();
                        }
                    })['finally'](function() {
                        $scope.editUserTitle = $scope.user.name;
                        blockUI.stop();
                    });
                } else {
                    var newUser = angular.copy($scope.user);
                    newUser.password = $scope.passwords.password;
                    if (angular.isUndefined(newUser.enabled)) {
                        newUser.enabled = false;
                    }
                    userManagement.createUser(newUser, errorCallback).then(function(response) {
                        if (response.data.status === 'UNIQUE_CONSTRAINTS_VIOLATION') {
                            showErrorMessage(unique_email);
                        } else if (response.data.status === 'INVALID_PASSWORD') {
                            showErrorMessage(invalid_password);
                        } else if (response.data.status === 'PASSWORD_NOT_STRONG_ENOUGH') {
                            showErrorMessage(response.data.message);
                        } else {
                            $location.path('edituser/' + $routeParams.page + '/' + response.data.id);
                        }
                    })['finally'](function() {
                        blockUI.stop();
                    });
                }
            }
        };
        
        var getRolesNotMemberOf = function() {
            var allRoles = ['admin','resource_manager'];
            var currentRoles = [];
            angular.forEach($scope.roles,function(value) {
                currentRoles.push(value.roleType);
            },currentRoles);
            var diffRoles = [];
            allRoles.forEach(function(key) {
                if (-1 === currentRoles.indexOf(key)) {
                    diffRoles.push(key);
                }
            }, this);
            var rolesNotMemberOf = [];
            angular.forEach(diffRoles,function(value) {
                var role = {};
                role.roleType = value;
                if (value === 'admin') {
                    role.name = admin_role_name;
                } else if (value === 'resource_manager') {
                    role.name = resource_manager_role_name;
                }
                rolesNotMemberOf.push(role);
            });
            return rolesNotMemberOf;
        }; 

        $scope.addRoles = function() {
            var rolesThisUserNotMemberOf = getRolesNotMemberOf();
            var modalInstance = $modal.open({
                backdrop : 'static',
                templateUrl : '/assets/javascripts/user/addRolesToUserDialog.html',
                controller : 'AddRolesToUserDialogController',
                scope: $scope,
                resolve : {
                    user : function() {
                        return $scope.user;
                    },
                    rolesNotMemberOf : function() {
                        return rolesThisUserNotMemberOf;
                    }
                }
            });
            modalInstance.result.then(function() {
                loadUser($routeParams.id);
            });
        };
        
        $scope.removeFromRole = function(role) {
            blockUI.start();
            var errorCallback = function() {
                showErrorMessage(deleting_role);
            };
            userManagement.deleteRoleMember(role.userId,role.roleType, errorCallback).then(function(data) {
                loadUser($routeParams.id);
            })['finally'](function() {
                blockUI.stop();
            });
        };
        
        $scope.status.isNewUser = false;
        
        if ($routeParams.id !== 'new') {            
            loadUser($routeParams.id);
        } else {
            $scope.status.isNewUser = true;
            $scope.user = {};
            $scope.user.id = 'new';
            $scope.editUserTitle = new_user_title;
        }

    };

    EditUserController.$inject = [ '$scope', '$location', '$routeParams', 'blockUI', '$modal', 
                                   'userManagement', '$translate', '$translatePartialLoader'];

    // ****** Start Add Roles To User Dialog Controller ******
    
    var AddRolesToUserDialogController = ['$scope', '$modalInstance', '$modal', 'userManagement', 'messageDialog', 'user', 'rolesNotMemberOf',
                                          '$translate', '$translatePartialLoader', 
                                          function($scope, $modalInstance, $modal, userManagement, 
                                           messageDialog, user, rolesNotMemberOf, $translate, $translatePartialLoader) {

        $scope.error = {};
        
        $scope.saveInProgress = false;
        $scope.addRoles = {};
        $scope.addRoles.availableRoles =  rolesNotMemberOf;
        $scope.addRoles.selectedRoles = {};

        // For testing, set to defaults
        var please_select_roles = "Please select one or more roles";
        var error_adding_roles = "Error adding role(s)";
        var add_role_unique_constraints_violation = "Unable to add role(s) since user already is a member";
        
        var loadi18n = function() {
            $translate(['please_select_roles','error_adding_roles',
                        'add_user_unique_constraints_violation']).then(function (translations) {
                            please_select_roles = translations.please_select_roles;
                            error_adding_roles = translations.error_adding_roles;
                            add_role_unique_constraints_violation = translations.add_role_unique_constraints_violation;
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
                
        $scope.close = function() {
            $modalInstance.close(undefined);
        };
      
        $scope.ok = function() {
            clearErrorMessage();
            if ($scope.addRoles.selectedRoles.length === 0) {
                showErrorMessage(please_select_roles);
                return;
            }
            $scope.saveInProgress = true;
            var errorCallback = function() {
                showErrorMessage(error_adding_roles);
            };
            var selectedUsers = [];
            angular.forEach($scope.addRoles.selectedRoles, function(value) {
                var selectedUser = {};
                selectedUser.userId = user.id;
                selectedUser.email = user.email;
                selectedUser.name = user.name;
                selectedUser.roleType = value.roleType;
                selectedUsers.push(selectedUser);
            },selectedUsers);
            userManagement.addRoleMembers(selectedUsers, errorCallback).then(function(response) {
                if (response.data.status === 'UNIQUE_CONSTRAINTS_VIOLATION') {
                    showErrorMessage(add_role_unique_constraints_violation);
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
        EditUserController : EditUserController,
        AddRolesToUserDialogController: AddRolesToUserDialogController
    };

    
});