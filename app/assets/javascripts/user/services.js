/**
 * User service, exposes user model to the rest of the app.
 */
define(['angular', 'moment', 'common'], function (angular,moment) {
  'use strict';

  var mod = angular.module('user.services', ['myapp.common', 'ngCookies']);
  mod.factory('userSessionContext', ['$q', 'playRoutes', '$cookies', '$log', '$location', '$rootScope',function ($q, playRoutes, $cookies, $log, $location, $rootScope) {
    var user, token = $cookies['XSRF-TOKEN'], currentRange;

    /*
	 * If the token is assigned, check that the token is still valid on the
	 * server
	 */
    if (token) {
      $log.info('Restoring user from cookie...');
      playRoutes.controllers.Users.currentLoggedInUser().get()
      .success(function (data) {
        $log.info('Welcome back, ' + data.name);
        user = data;
        setUserName(data.name);
      })
      .error(function () {
        $log.info('Token no longer valid, please log in.');
        token = undefined;
        delete $cookies['XSRF-TOKEN'];
        $location.path('/login');
        return $q.reject("Token invalid");
      });
    }

    function setUserName(userName) {
        $rootScope.userName = userName;
    }
    
    return {
       loginUser: function (credentials,errorCallback) {
           var loginPromise = playRoutes.controllers.Application.login().post(credentials);
           loginPromise.error(errorCallback);
           return loginPromise.then(function (response) {
              token = response.data.token;
              user = response.data.user;
              setUserName(response.data.user.name);
              return user;
           });
       },
       logout: function () {
            return playRoutes.controllers.Application.logout().post().then(function () {
              delete $cookies['XSRF-TOKEN'];
              token = undefined;
              user = undefined;
              setUserName(undefined);
              $location.path('/login');
            });
       },
       getLoggedInUser: function () {
          return user;
       },
       isUserLoggedIn: function() {
          if (token) {
             return true;
          }
          return false;
       },
       currentRange : function() {
		  if (currentRange === undefined) {
		     var todaysDate = moment();
		     var range = {};
		     range.startDayOfMonth = todaysDate.weekday(0).date();
		     range.startMonth = todaysDate.month();
		     range.startYear = todaysDate.year();

		     var endDay = todaysDate.weekday(6);
			 range.endDayOfMonth = endDay.date();
			 range.endMonth = endDay.month();
			 range.endYear = endDay.year();

			currentRange = range;
		  }
		  return currentRange;
	   }
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
             var updateUserPromise = playRoutes.controllers.Users.changeMyPassword(currentPassword,newPassword).post();
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
