/**
 * Equivalent to JOptionPane in Swing.
 */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.message-dialog', []);
  mod.service('messageDialog', function() {
      return {
          showMessage: function($uibModal,title,message) {
              var dialogTemplate = '<div class="modal-header"><h4>{{title}}</h4></div><div class="modal-body">{{msg}}</div><div class="modal-footer"><button ng-click="close()" class="btn">Close</button></div>';
              $uibModal.open({
                 template: dialogTemplate,
                 backdrop: 'static',
                 controller: 'MessageDialogController',           
                   resolve: {
                     msg: function () { return message;},
                     title: function () { return title;}
                   }
               });
          },
          showConfirmationMessage: function($uibModal,confirmationCallback,title,message) {
              var dialogTemplate = '<div class="modal-header"><h4>{{title}}</h4></div><div class="modal-body">{{msg}}</div><div class="modal-footer"><button ng-click="yes()" class="btn">Yes</button><button ng-click="no()" class="btn">No</button></div>';
              var modalInstance = $modal.open({
                 template: dialogTemplate,
                 backdrop: 'static',
                 controller: 'ConfirmationMessageDialogController',           
                   resolve: {
                     msg: function () { return message;},
                     title: function () { return title;}
                   }
               });
               modalInstance.result.then(confirmationCallback,function(){});
          }
      };
  });
  
  function MessageDialogController($scope, $uibModalInstance, title, msg) {

      $scope.msg = msg;
      $scope.title = title;
      
      $scope.close = function () {
          $uibModalInstance.dismiss('ok');
      };
      
  }
  
  function ConfirmationMessageDialogController($scope, $uibModalInstance, title, msg) {

      $scope.msg = msg;
      $scope.title = title;
      
      $scope.no = function () {
          $uibModalInstance.dismiss('no');
      };
      
      $scope.yes = function () {
          $uibModalInstance.close('yes');
      };
      
  }
  
  mod.controller('MessageDialogController', ['$scope', '$uibModalInstance', 'title', 'msg',MessageDialogController]);
  mod.controller('ConfirmationMessageDialogController', ['$scope', '$uibModalInstance', 'title', 'msg',ConfirmationMessageDialogController]);
  
});