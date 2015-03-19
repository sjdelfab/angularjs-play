define([ 'angular', 'angularMocks','app','common'], 
        function(angular, mocks) {
    'use strict';

    describe("Validate Popups Directive Test", function() {

        var $scope, $compile, compileEl;
        beforeEach(function() {
            module('app');
            
            inject(function(_$compile_, $rootScope) {
                $scope = $rootScope.$new();
                $compile = _$compile_;
            });
            
        });
        
        compileEl = function() {
            var el;
            el = $compile('<form name="userForm" data-form-validation>\
                <div id="first-name-group" class="form-group" >\
                  <input type="text" name="firstName" ng-model="firstName" ng-minlength="3" class="form-control" data-validate-popups/>\
                </div>\
                <div id="last-name-group" class="form-group">\
                  <input type="text" name="lastName" ng-model="lastName" ng-minlength="3" class="form-control" data-validate-popups/>\
                </div>\
              </form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        describe('directive does not contain an outer form', function() {
            return it('throws an exception', function() {
              return expect(function() {
                return $compile('<div class="form-group"><input type="text" name="firstName" data-validate-popups></input></div>')($scope);
              }).toThrow("Must have an outer form");
            });
        });
        
        describe('directive outer form does not contain a name', function() {
            return it('throws an exception', function() {
              return expect(function() {
                return $compile('<form data-form-validation><div class="form-group"><input type="text" name="firstName" data-validate-popups></input></div></form')($scope);
              }).toThrow("Outer form must have a name");
            });
        });
        
        describe('directive element not contain a name', function() {
            return it('throws an exception', function() {
              return expect(function() {
                return $compile('<form name="userForm" data-form-validation><div class="form-group"><input type="text" data-validate-popups></input></div></form')($scope);
              }).toThrow("Element must have a name");
            });
        });
        
        describe('directive outer form must have the form-validation attribute', function() {
            return it('throws an exception', function() {
              return expect(function() {
                return $compile('<form name="userForm"><div class="form-group"><input type="text" name="firstName" data-validate-popups></input></div></form')($scope);
              }).toThrow("Outer form must have form-validation attribute");
            });
        });
        
        describe('OK', function() {
            return it('Has correct attributes added', function() {
              var el;
              
              el = compileEl();
              expect(angular.element(firstNameEl(el)).attr('popover-placement')).toBe('right');
              expect(angular.element(firstNameEl(el)).attr('popover-trigger')).toBe('focus');
              $scope.userForm.validationMessages.firstName = 'This is an error';
              $scope.$digest();
              expect(angular.element(firstNameEl(el)).attr('popover')).toBe('This is an error');
            });
        });
    });
    
    var find = function(el, selector) {
        return el[0].querySelector(selector);
    };

    var firstNameEl = function(el) {
        return find(el, '[name=firstName]');
    };

});

