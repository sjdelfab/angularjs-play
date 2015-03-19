define([ 'angular' ], function(angular) {
    'use strict';
   
    var UsersController = function($scope, $location, $routeParams, blockUI, userManagement, 
                                   $modal, messageDialog, $translate, $translatePartialLoader) {

        // defaults
        var loading_error_message = "Error loading users";
        var deleting_error_message = "Error deleting user";
        var disabling_error_message = "Error disabling user";
        var enabling_error_message = "Error enabling user";
        var unlocking_error_message = "Error unlocking user";
        var delete_user_title = "Delete user";
        var saving_progress_message = "Saving...";
        var foreign_key_violation = "User cannot be deleted because it is being used. Disable instead";
 
        var loadi18n = function() {
            $translate(['manage_users.error_messages.loading','manage_users.error_messages.deleting',
                        'manage_users.error_messages.disabling','manage_users.error_messages.enabling',
                        'manage_users.error_messages.foreign_key_violation',
                        'manage_users.error_messages.unlocking','manage_users.delete_user_title',
                        'saving_progress_message']).then(function (translations) {
                loading_error_message = translations['manage_users.error_messages.loading'];
                deleting_error_message = translations['manage_users.error_messages.deleting'];
                disabling_error_message = translations['manage_users.error_messages.disabling'];
                enabling_error_message = translations['manage_users.error_messages.enabling'];
                unlocking_error_message = translations['manage_users.error_messages.unlocking'];
                delete_user_title = translations['manage_users.delete_user_title'];
                saving_progress_message = translations.saving_progress_message;
                foreign_key_violation = translations['manage_users.error_messages.foreign_key_violation'];
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
        $scope.error.message = '';
        $scope.error.displayErrorMessage = false;
        $scope.users = [];
        $scope.page = $routeParams.page;

        $scope.enabledClass = "glyphicon glyphicon-ok user-enabled-icon";
        $scope.disabledClass = "glyphicon glyphicon-remove user-disabled-icon";
        $scope.accountLockedClass = "fa fa-lock";

        $scope.enabledClasses = {};

        $scope.currentPage = 1;
        $scope.maxSize = 5;

        var showErrorMessage = function(message) {
            $scope.error.displayErrorMessage = true;
            $scope.error.message = message;
        };

        var setResults = function(data) {
            $scope.users = data.users;
            $scope.totalItems = data.total;
            $scope.numberOfPages = data.numberOfPages;
            $scope.pageSize = data.pageSize;
        };

        $scope.refreshPage = function(page) {
            loadUsers(page);
        };

        var loadUsers = function(page) {
            blockUI.start();
            var errorCallback = function() {
                showErrorMessage(loading_error_message);
            };
            userManagement.getUsers(page, errorCallback).then(function(data) {
                setResults(data);
            })['finally'](function() {
                blockUI.stop();
            });
        };

        $scope.changePassword = function(user) {
            $modal.open({
                backdrop : 'static',
                templateUrl : '/assets/javascripts/user/changePassword.html',
                controller : 'ChangePasswordController',
                scope: $scope,
                resolve : {
                    user : function() {
                        return user;
                    },
                    changePasswordFunction: function() {
                        return userManagement.changeUserPassword;
                    }
                }
            });
        };

        $scope.deleteUser = function(user) {
            var confirmationCallback = function() {
                blockUI.start(saving_progress_message);
                var errorCallback = function() {
                    showErrorMessage(deleting_error_message + ': ' + user.name);
                };
                userManagement.deleteUser(user.id, errorCallback).then(function(response) {
                    if (response.data.status === 'FK_CONSTRAINTS_VIOLATION') {
                        showErrorMessage(foreign_key_violation);
                    } else {
                        loadUsers($routeParams.page);
                    }
                })['finally'](function() {
                    blockUI.stop();
                });
            };
            var title = delete_user_title;            
            var message = "Do you wish to delete user " + user.name; // default
            $translate('manage_users.delete_user_confirmation_message', { userName: user.name }).then(function (translations) {
                message = translations['manage_users.delete_user_confirmation_message'];
            });
            
            messageDialog.showConfirmationMessage($modal,confirmationCallback,title,message);
        };

        $scope.disableUser = function(user) {
            userServerOperation(userManagement.disableUser,user,disabling_error_message);
        };

        $scope.enableUser = function(user) {
            userServerOperation(userManagement.enableUser,user,enabling_error_message);
        };

        $scope.unlockUser = function(user) {
            userServerOperation(userManagement.unlockUser,user,unlocking_error_message);
        };
        
        var userServerOperation = function(serverFunction,user,errorMessage) {
            blockUI.start(saving_progress_message);
            var errorCallback = function() {
                showErrorMessage(errorMessage + ': ' + user.name);
            };
            serverFunction(user.id, errorCallback).then(function() {
                loadUsers($routeParams.page);
            })['finally'](function() {
                blockUI.stop();
            });
        };

        $scope.createNewUser = function() {
            $location.path('edituser/' + $routeParams.page + '/new');
        };

        loadUsers($routeParams.page);
    };
    
    UsersController.$inject = [ '$scope', '$location', '$routeParams', 'blockUI', 'userManagement', '$modal', 
                                'messageDialog', '$translate', '$translatePartialLoader'];

    
    var ChangePasswordController = ['$scope', '$modalInstance', '$modal', 'messageDialog',
                                    '$translate', '$translatePartialLoader', 'userManagement', 'user',
                                    function($scope, $modalInstance, $modal, messageDialog, 
                                             $translate, $translatePartialLoader, userManagement, user) {

        var password_do_not_match = "Passwords do not match";
        var error_changing_password = "Error updating user";
        var invalid_password_message = "Only ASCII characters allowed";
        var changed_password_title = "Changed password";
        
        var loadi18n = function() {
            $translate(['change_password_dialog.password_do_not_match',
                        'change_password_dialog.error_changing_password',
                        'change_password_dialog.invalid_password_message',
                        'change_password_dialog.changed_password_title']).then(function (translations) {
                password_do_not_match = translations['change_password_dialog.password_do_not_match'];                            
                error_changing_password = translations['change_password_dialog.error_changing_password'];
                invalid_password_message = translations['change_password_dialog.invalid_password_message'];
                changed_password_title = translations['change_password_dialog.changed_password_title'];
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
        
        $scope.user = user;
        $scope.saveInProgress = false;
        $scope.passwords = {};
        $scope.passwords.password = undefined;
        $scope.passwords.retypePassword = undefined;

        $scope.close = function() {
            $modalInstance.close(undefined);
        };

        var validatePasswords = function() {
            if ($scope.passwords.password !== $scope.passwords.retypePassword) {
                showErrorMessage(password_do_not_match);
                return false;
            }
            return true;
        };

        $scope.ok = function() {
            if (!validatePasswords()) {
                return;
            }
            $scope.saveInProgress = true;
            var errorCallback = function() {
                showErrorMessage(error_changing_password);
            };
            userManagement.changeUserPassword($scope.user.id, $scope.passwords.password, errorCallback).then(function(response) {
                if (response.data.status === 'INVALID_PASSWORD') {
                    showErrorMessage(invalid_password_message);
                } else if (response.data.status === 'PASSWORD_NOT_STRONG_ENOUGH') {
                    showErrorMessage(response.data.message);
                } else {
                    $scope.close();
                    var message = 'Successfully changed password for ' + user.name;
                    $translate('change_password_dialog.successfully_changed_password', { userName: user.name }).then(function (translations) {
                        message = translations['change_password_dialog.successfully_changed_password'];
                    });
                    messageDialog.showMessage($modal,changed_password_title,message);
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
        UsersController: UsersController,
        ChangePasswordController : ChangePasswordController
    };
});