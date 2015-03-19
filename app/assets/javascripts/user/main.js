define(['angular','./loginController','./manageUsersController','./manageRolesControllers', 
        './editUserController', './myProfileController', 
        './routes', './services'], 
        function(angular,loginController, manageUsersController, manageRolesControllers, 
                editUserController, myProfileController) {
  'use strict';

  var mod = angular.module('myapp.user', ['ngCookies', 'ngRoute', 'user.routes', 'user.services','ui.bootstrap',
                                          'common.message-dialog','ui.select','ngSanitize','pascalprecht.translate']);
  mod.controller('LoginController', loginController);
  mod.controller('UsersController', manageUsersController.UsersController);
  mod.controller('EditUserController', editUserController.EditUserController);
  mod.controller('ChangePasswordController', manageUsersController.ChangePasswordController);
  mod.controller('UserRolesController', manageRolesControllers.UserRolesController);
  mod.controller('EditUserRoleController', manageRolesControllers.EditUserRoleController);
  mod.controller('AddUsersToRoleDialogController', manageRolesControllers.AddUsersToRoleDialogController);
  mod.controller('AddRolesToUserDialogController', editUserController.AddRolesToUserDialogController);
  mod.controller('MyProfileController', myProfileController.MyProfileController);
  mod.controller('ChangeOwnPasswordController', myProfileController.ChangeOwnPasswordController);
  return mod;
});
