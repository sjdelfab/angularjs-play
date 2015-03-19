/**
 * Configure routes of user module.
 */
define(['angular','./loginController', './manageUsersController', './editUserController', './manageRolesControllers', 
        './myProfileController', 'common'], 
        function(angular, loginController, manageUsersController, editUserController, manageRolesControllers, myProfileController) {
  'use strict';

  var mod = angular.module('user.routes', ['user.services', 'myapp.common']);
  mod.config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/login', {templateUrl:'/assets/javascripts/user/login.html', controller:loginController})
      .when('/users/:page', {templateUrl:'/assets/javascripts/user/users.html', controller:manageUsersController.UsersController})
      .when('/edituser/:page/:id', {templateUrl:'/assets/javascripts/user/editUser.html', controller:editUserController.EditUserController})
      .when('/userroles', {templateUrl:'/assets/javascripts/user/userRoles.html', controller:manageRolesControllers.UserRolesController})
      .when('/edituserrole/:role', {templateUrl:'/assets/javascripts/user/editRole.html', controller:manageRolesControllers.EditUserRoleController})
      .when('/myprofile', {templateUrl:'/assets/javascripts/user/myProfile.html', controller:myProfileController.MyProfileController});
  }]);
  return mod;
});
