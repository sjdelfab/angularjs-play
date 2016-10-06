define([ 'angular' ], function(angular) {
    'use strict';
   
    var MyProfileController = function($scope, $location, $routeParams, blockUI, $uibModal, 
                                      userManagement,$translate, $translatePartialLoader) {

        $scope.error = {};
        $scope.error.message = '';
        $scope.error.displayErrorMessage = false;

        $scope.status = {};
        $scope.status.message = 'Successfully updated';
        $scope.status.displayMessage = false;

        $scope.page = $routeParams.page;
        
        $scope.passwords = {};
        $scope.passwords.password = undefined;
        $scope.passwords.retypePassword = undefined;

        $scope.roles = {};
        
        var saving_progress_message = "Saving...";
        var admin_role_name = "Administrator";
        var resource_manager_role_name = "Resource Manager";
        var error_loading = "Error obtaining user";
        var error_updating = "Error updating user";
        
        var loadi18n = function() {
            $translate(['saving_progress_message', 'admin_role_name', 'resource_manager_role_name',
                        'edit_user.messages.successful_update',
                        'edit_user.messages.error_loading',
                        'edit_user.messages.error_updating']).then(function (translations) {
                saving_progress_message = translations.saving_progress_message;
                admin_role_name = translations.admin_role_name;
                resource_manager_role_name = translations.resource_manager_role_name;
                $scope.status.message = translations['edit_user.messages.successful_update'];
                error_loading = translations['edit_user.messages.error_loading'];
                error_updating = translations['edit_user.messages.error_updating'];
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
        
        var loadUser = function() {
            var errorCallback = function() {
                showErrorMessage(error_loading);
            };
            blockUI.start();
            userManagement.getMyProfile(errorCallback).then(function(data) {
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
                userManagement.updateMyProfile($scope.user, errorCallback).then(function(response) {
                    showUpdatedMessage();
                })['finally'](function() {
                    $scope.editUserTitle = $scope.user.name;
                    blockUI.stop();
                });
            }
        };
        
        $scope.changeMyPassword = function(user) {
            $uibModal.open({
                backdrop : 'static',
                templateUrl : '/assets/javascripts/user/changeOwnPassword.html',
                controller : 'ChangeOwnPasswordController',
                scope: $scope,
                resolve : {
                    user : function() {
                        return $scope.user;
                    }
                }
            });
        };
        
        loadUser();

    };

    MyProfileController.$inject = ['$scope', '$location', '$routeParams', 'blockUI', '$uibModal', 
                                   'userManagement', '$translate', '$translatePartialLoader'];
    
    var ChangeOwnPasswordController = ['$scope', '$uibModalInstance', '$uibModal', 'messageDialog',
                                    '$translate', '$translatePartialLoader', 'userManagement', 'user',
                                    function($scope, $uibModalInstance, $uibModal, messageDialog, 
                                             $translate, $translatePartialLoader, userManagement, user) {

        var password_do_not_match = "Passwords do not match";
        var error_changing_password = "Error updating user";
        var invalid_password_message = "Only ASCII characters allowed";
        var changed_password_title = "Changed password";
        var incorrect_current_password = "Incorrect current password";
        
        var loadi18n = function() {
            $translate(['change_password_dialog.password_do_not_match',
                        'change_password_dialog.error_changing_password',
                        'change_password_dialog.invalid_password_message',
                        'change_password_dialog.changed_password_title',
                        'change_own_password_dialog.incorrect_current_password']).then(function (translations) {
                password_do_not_match = translations['change_password_dialog.password_do_not_match'];                            
                error_changing_password = translations['change_password_dialog.error_changing_password'];
                invalid_password_message = translations['change_password_dialog.invalid_password_message'];
                changed_password_title = translations['change_password_dialog.changed_password_title'];
                incorrect_current_password = translations['change_own_password_dialog.incorrect_current_password'];
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
        $scope.passwords.currentPassword = undefined;
        $scope.passwords.password = undefined;
        $scope.passwords.retypePassword = undefined;

        $scope.close = function() {
            $uibModalInstance.dismiss('close');
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
            userManagement.changeMyPassword($scope.passwords.currentPassword, $scope.passwords.password, errorCallback).then(function(response) {
                if (response.data.status === 'INVALID_PASSWORD') {
                    showErrorMessage(invalid_password_message);
                } else if (response.data.status === 'PASSWORD_NOT_STRONG_ENOUGH') {
                    showErrorMessage(response.data.message);
                } else if (response.data.status === 'INVALID_CURRENT_PASSWORD') {
                    showErrorMessage(incorrect_current_password);
                } else {
                    $uibModalInstance.close('ok');
                    var message = 'Successfully changed password';
                    $translate('change_own_password_dialog.successfully_changed_password').then(function (translations) {
                        message = translations['change_own_password_dialog.successfully_changed_password'];
                    });
                    messageDialog.showMessage($uibModal,changed_password_title,message);
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
        MyProfileController : MyProfileController,
        ChangeOwnPasswordController: ChangeOwnPasswordController
    };
    
}); 