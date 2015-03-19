define([ 'angular', 'angularMocks','app','common'], 
        function(angular, mocks) {
    'use strict';

    describe("Validate Form Directive Test", function() {

        var $scope, $compile, compileEl, compileElUsingDots, compileInvalidEl;
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
                  <input type="text" name="firstName" ng-model="firstName" data-ng-minlength="3" data-ng-maxlength="10" class="form-control" data-validate-popups/>\
                </div>\
                <div id="last-name-group" class="form-group">\
                  <input type="text" name="lastName" ng-model="lastName" ng-minlength="3" class="form-control" data-validate-popups required/>\
                </div>\
            	<div id="email-group" class="form-group">\
                    <input type="email" name="email" ng-model="email" class="form-control" data-validate-popups/>\
                </div>\
            	<div id="id-group" class="form-group">\
                    <input type="text" name="id" ng-model="id" class="form-control" ng-pattern="/^\d+$/" data-invalid-pattern-message="Must only be numbers" data-validate-popups/>\
                </div>\
            	</form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        compileElUsingDots = function() {
            var el;
            el = $compile('<form name="users.userForm" data-form-validation>\
                <div id="first-name-group" class="form-group" >\
                  <input type="text" name="firstName" ng-model="users.firstName" data-ng-minlength="3" data-ng-maxlength="10" class="form-control" data-validate-popups/>\
                </div>\
                </form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        compileInvalidEl = function() {
            var el;
            el = $compile('<form name="userForm" data-form-validation>\
            	<div id="id-group" class="form-group">\
                    <input type="text" name="id" ng-model="id" class="form-control" ng-pattern="/^\d+$/" data-validate-popups/>\
                </div>\
            	</form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        describe('ng-pattern input does not have data-invalid-pattern-message attribute', function() {
            it('throws an exception', function() {
                var formEl;
                formEl = compileInvalidEl();
                $scope.userForm.id.$setViewValue('to');

                return expect(function() {
                   expect($scope.userForm.validateForm()).toBe(false);
                }).toThrow("Must specify an invalid pattern message");
            });
        });
        
        describe('Invalid', function() {
	        it('Minimum length is invalid', function() {
	           var el;
	           el = compileEl();
	           $scope.userForm.firstName.$setViewValue('to');
	           expect($scope.userForm.validateForm()).toBe(false);
	           $scope.$digest();
	           expect(angular.element(firstNameEl(el)).attr('popover')).toBe('Minimum length is 3 characters');
	           return expectFirstNameFormGroupHasErrorClass(el).toBe(true);
	        });
	        
	        it('Maximum length is invalid', function() {
	            var el;
	            el = compileEl();
	            $scope.userForm.firstName.$setViewValue('12345678901');
	            expect($scope.userForm.validateForm()).toBe(false);
	            $scope.$digest();
	            expect(angular.element(firstNameEl(el)).attr('popover')).toBe('Maximum length is 10 characters');
	            return expectFirstNameFormGroupHasErrorClass(el).toBe(true);
	        });
	        
	        it('Required is invalid', function() {
	            var el;
	            el = compileEl();
	            $scope.userForm.firstName.$setViewValue('Simon');
	            expect($scope.userForm.validateForm()).toBe(false);
	            $scope.$digest();
	            expect(angular.element(lastNameEl(el)).attr('popover')).toBe('This is a required field');
	            return expectLastNameFormGroupHasErrorClass(el).toBe(true);
	        });
	        
	        it('Email is invalid', function() {
	            var el;
	            el = compileEl();
	            $scope.userForm.firstName.$setViewValue('Simon');
	            $scope.userForm.lastName.$setViewValue('Del');
	            $scope.userForm.email.$setViewValue('simon@');
	            expect($scope.userForm.validateForm()).toBe(false);
	            $scope.$digest();
	            expect(angular.element(emailEl(el)).attr('popover')).toBe('Invalid email address');
	            return expectEmailFormGroupHasErrorClass(el).toBe(true);
	        });
	        
	        it('Pattern is invalid', function() {
	            var el;
	            el = compileEl();
	            $scope.userForm.firstName.$setViewValue('Simon');
	            $scope.userForm.lastName.$setViewValue('Del');
	            $scope.userForm.id.$setViewValue('foo');
	            expect($scope.userForm.validateForm()).toBe(false);
	            $scope.$digest();
	            expect(angular.element(idEl(el)).attr('popover')).toBe('Must only be numbers');
	            return expectIdFormGroupHasErrorClass(el).toBe(true);
	        });
        });
        
        describe('Invalid using dot notation', function() {
            beforeEach(function() {
                $scope.users = {};
            });
            
            it('Minimum length is invalid', function() {
               var el;
               el = compileElUsingDots();
               $scope.users.userForm.firstName.$setViewValue('to');
               expect($scope.users.userForm.validateForm()).toBe(false);
               $scope.$digest();
               expect(angular.element(firstNameEl(el)).attr('popover')).toBe('Minimum length is 3 characters');
               return expectFirstNameFormGroupHasErrorClass(el).toBe(true);
            });
        });
    });
    
    var find = function(el, selector) {
        return el[0].querySelector(selector);
    };

    var firstNameEl = function(el) {
        return find(el, '[name=firstName]');
    };

    var lastNameEl = function(el) {
        return find(el, '[name=lastName]');
    };
    
    var emailEl = function(el) {
        return find(el, '[name=email]');
    };
    
    var idEl = function(el) {
        return find(el, '[name=id]');
    };
    
    var expectFirstNameFormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=first-name-group]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };
        
    var expectLastNameFormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=last-name-group]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };
    
    var expectEmailFormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=email-group]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };

    var expectIdFormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=id-group]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };
});