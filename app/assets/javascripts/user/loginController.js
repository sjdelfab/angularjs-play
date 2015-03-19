define([ 'angular' ], function(angular) {
    'use strict';

    var LoginController = function($scope, $location, userSessionContext, $translate, $translatePartialLoader) {
        
        $scope.error = {};
        $scope.error.loginErrorMessage = '';
        $scope.error.displayErrorMessage = false;
        $scope.loginInProgress = false;
        $scope.credentials = {};

        // For testing, set to defaults
        var invalid_username_or_password_message = "Invalid username or password";
        var account_locked_message = "Account Locked";
        var unable_to_login_message = "Unable to login";
        var provide_email_or_password_message = "Please provide an email and password";
        
        var loadi18n = function() {
            $translate(['invalid_username_or_password_message','account_locked_message',
                        'unable_to_login_message','provide_email_or_password_message']).then(function (translations) {
                            invalid_username_or_password_message = translations.invalid_username_or_password_message;
                            account_locked_message = translations.account_locked_message;
                            unable_to_login_message = translations.unable_to_login_message;
                            provide_email_or_password_message = translations.provide_email_or_password_message;
            });
        };

        if (!$translatePartialLoader.isPartAvailable('login')) {
            $translatePartialLoader.addPart('login');
            $translate.refresh().then(function() {
                loadi18n();
            });
        } else {
            loadi18n();
        }
        
        $scope.login = function(credentials) {
            if (validateCredentials(credentials)) {
                $scope.loginInProgress = true;
                var errorCallback = function(loginResult) {
                    $scope.loginInProgress = false;
                    if (angular.isDefined(loginResult) && angular.isDefined(loginResult.login_error_status)) {
                        showErrorMessage(getLoginErrorMessage(loginResult.login_error_status));
                    } else {
                        showErrorMessage(unable_to_login_message);
                    }
                };
                // Chaining promises: http request promise and the chained promise. Error callback needs to be called when http promise fails
                userSessionContext.loginUser(credentials, errorCallback).then(function(/*user*/) {
                    $location.path('/');
                    $scope.loginInProgress = false;
                });
            }
        };

        var getLoginErrorMessage = function(loginErrorStatus) {
            if (loginErrorStatus == 'INVALID_USERNAME_PASSWORD') {
                return invalid_username_or_password_message;
            } else if (loginErrorStatus == 'ACCOUNT_LOCKED') {
                return account_locked_message;
            }
            return unable_to_login_message;
        };

        var validateCredentials = function(credentials) {
            if (angular.isUndefined(credentials) || angular.isUndefined(credentials.email) || angular.isUndefined(credentials.password)) {
                showErrorMessage(provide_email_or_password_message);
                return false;
            }
            return true;
        };

        var showErrorMessage = function(message) {
            $scope.error.displayErrorMessage = true;
            $scope.error.loginErrorMessage = message;
        };
    };
    
    LoginController.$inject = [ '$scope', '$location', 'userSessionContext','$translate', '$translatePartialLoader'];
    
    return LoginController;
    
});
