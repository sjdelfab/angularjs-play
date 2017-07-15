/**
 * User service, exposes user model to the rest of the app.
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('user.services', ['myapp.common', 'ngCookies', 'satellizer']);
  mod.factory('userSessionContext', ['$q', 'playRoutes', '$cookies', '$log', '$location', '$rootScope','$auth',function ($q, playRoutes, $cookies, $log, $location, $rootScope, $auth) {
    var user;

    if ($auth.isAuthenticated()) {
      $log.info('Restoring user...');
      playRoutes.controllers.Users.currentLoggedInUser().get()
      .success(function (data) {
        $log.info('Welcome back, ' + data.name);
        user = data;
        setUserName(data.name);
      })
      .error(function () {
        $log.info('Not logged in.');
        $cookies.remove('PLAY_CSRF_TOKEN');
        $location.path('/login');
        return $q.reject("Not logged in");
      });
    }

    function setUserName(userName) {
        $rootScope.userName = userName;
    }
    
    return {
       loginUser: function (credentials,errorCallback) {
           var loginPromise = $auth.login(credentials);
           loginPromise.catch(errorCallback);
           return loginPromise.then(function (response) {
              user = response.data.user;
              setUserName(response.data.user.name);
              return user;
           });
       },
       logout: function () {
            return playRoutes.controllers.Application.logout().post().then(function () {
              $cookies.remove('PLAY_CSRF_TOKEN');
              user = undefined;
              setUserName(undefined);
              $auth.logout();
              $location.path('/login');
            });
       },
       getLoggedInUser: function () {
          return user;
       },
       isUserLoggedIn: function() {          
          return $auth.isAuthenticated();
       },
    };
  }]).factory('userManagement', ['playRoutes', function (playRoutes) {
      return {
         getUsers: function(page,errorCallback) {
             var getUsersPromise = playRoutes.controllers.Users.getUsers(page).get();
             getUsersPromise.error(errorCallback);
             return getUsersPromise.then(function (response) {
                return response.data;
             });
         },
         getUser: function(externalisedUserId,errorCallback) {
             var getUserPromise = playRoutes.controllers.Users.getUser(externalisedUserId).get();
             getUserPromise.error(errorCallback);
             return getUserPromise.then(function (response) {
                return response.data;
             });
         },
         updateUser: function(user,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.updateUser().post(user);
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         disableUser: function(userId,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.enableUser(userId,false).post();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         enableUser: function(userId,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.enableUser(userId,true).post();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         unlockUser: function(userId,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.unlockUser(userId).post();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         deleteUser: function(userId,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.deleteUser(userId).delete();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         changeUserPassword: function(userId,newPassword,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.changeUserPassword(userId,newPassword).post();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         createUser: function(user,errorCallback) {
             var createUserPromise = playRoutes.controllers.Users.createUser().post(user);
             createUserPromise.error(errorCallback);
             return createUserPromise;
         },
         getRoleMembers: function(roleType,errorCallback) {
             var getRoleMembersPromise = playRoutes.controllers.Users.getRoleMembers(roleType).get();
             getRoleMembersPromise.error(errorCallback);
             return getRoleMembersPromise.then(function (response) {
                return response.data;
             });
         },
         getRoleNonMembers: function(roleType,errorCallback) {
             var getRoleNonMembersPromise = playRoutes.controllers.Users.getRoleNonMembers(roleType).get();
             getRoleNonMembersPromise.error(errorCallback);
             return getRoleNonMembersPromise.then(function (response) {
                return response.data;
             });
         },
         deleteRoleMember: function(userId,roleType,errorCallback) {
             var deleteRoleMemberPromise = playRoutes.controllers.Users.deleteRoleMember(userId,roleType).delete();
             deleteRoleMemberPromise.error(errorCallback);
             return deleteRoleMemberPromise;
         },
         addRoleMembers: function(newUsers,errorCallback) {
             var addNewUsersToRolePromise = playRoutes.controllers.Users.addUsersToRole().post(newUsers);
             addNewUsersToRolePromise.error(errorCallback);
             return addNewUsersToRolePromise;
         },
         getMyProfile: function(errorCallback) {
             var getUserPromise = playRoutes.controllers.Users.getMyProfile().get();
             getUserPromise.error(errorCallback);
             return getUserPromise.then(function (response) {
                return response.data;
             });
         },
         updateMyProfile: function(user,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Users.updateMyProfile().post(user);
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         },
         changeMyPassword: function(currentPassword,newPassword,errorCallback) {
             var updateUserPromise = playRoutes.controllers.Application.changeMyPassword(currentPassword,newPassword).post();
             updateUserPromise.error(errorCallback);
             return updateUserPromise;
         }
       };
    }]);
  
  /**
	 * Add this object to a route definition to only allow resolving the route
	 * if the user is logged in. This also adds the contents of the objects as a
	 * dependency of the controller.
	 */
  mod.constant('userResolve', {
    user: ['$q', 'userManagement', function ($q, userManagement) {
      var deferred = $q.defer();
      // TODO is this code being called?
      // Needs to first check that user is logged in
      var user = userManagement.getUser();
      if (user) {
        deferred.resolve(user);
      } else {
        deferred.reject();
      }
      return deferred.promise;
    }]
  });
  
  /**
   * If the current route does not resolve, go back to the start page.
   */
  var handleRouteError = function($rootScope, $location, userSessionContext) {
     $rootScope.$on('$routeChangeError',
        function(/* e, next, current */) {
           $location.path('/');
     });
  };
  
  handleRouteError.$inject = ['$rootScope', '$location', 'userSessionContext'];
  mod.run(handleRouteError);
  
  var locationChangeInit = function ($rootScope, $location, userSessionContext) {
      $rootScope.$on("$locationChangeStart", function (event, next, current) {
          if (userSessionContext.isUserLoggedIn()) {
              if ($location.path() == '/login') {                 
                  $location.path('/');
              } 
          } else {
              $location.path('/login');
          }
      });
  }; 
  
  locationChangeInit.$inject = ['$rootScope', '$location', 'userSessionContext'];
  mod.run(locationChangeInit);
  return mod;
});
