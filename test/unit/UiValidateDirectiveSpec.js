define([ 'angular', 'angularMocks','app','common'], 
        function(angular, mocks) {
    'use strict';

    describe("UI validate Form Directive Test", function() {

        var $scope, $compile, validatorCompileEl, customValidatorCompileEl;
        beforeEach(function() {
            module('app',function($provide) {
                $provide.value('playRoutes', mockPlayRoutes);
              }
            );
            
            inject(function(_$compile_, $rootScope) {
                $scope = $rootScope.$new();
                $compile = _$compile_;
            });
            
        });
        
        validatorCompileEl = function() {
            var el;
            el = $compile('<form name="userForm1" data-form-validation>\
                <div id="first-name-group1" class="form-group" >\
                  <input type="text" name="firstName1" ng-model="firstName1" data-ui-validate="\'$value === lastName1\'" ui-validate-watch="\'lastName1\'" class="form-control" data-ui-validate-message="\'This is invalid\'" data-validate-popups/>\
                </div>\
                <div id="last-name-group1" class="form-group">\
                  <input type="text" name="lastName1" ng-model="lastName1" class="form-control" data-validate-popups/>\
                </div>\
                </form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        customValidatorCompileEl = function() {
            var el = $compile('<form name="userForm2" data-form-validation>\
                <div id="first-name-group2" class="form-group" >\
                  <input type="text" name="firstName2" ng-model="firstName2" data-ui-validate="{ customError : \'$value === lastName2\' }" ui-validate-watch=" { customError : \'lastName2\' }" class="form-control" data-ui-validate-message="\'Custom invalid\'" data-validate-popups/>\
                </div>\
                <div id="last-name-group2" class="form-group">\
                  <input type="text" name="lastName2" ng-model="lastName2"  class="form-control" data-validate-popups/>\
                </div>\
                </form>')($scope);
            angular.element(document.body).append(el);
            $scope.$digest();
            return el;
        };
        
        it('Names same, so should be valid', function() {
            var formEl = validatorCompileEl();
            $scope.userForm1.firstName1.$setViewValue('name');
            $scope.userForm1.lastName1.$setViewValue('name');
            $scope.$digest();
            expect($scope.userForm1.firstName1.$valid).toBe(true);
            expect($scope.userForm1.$invalid).toBe(false)
            return expect($scope.userForm1.validateForm()).toBe(true);
        });
        
        it('Names different, so should be invalid', function() {
            var formEl = validatorCompileEl();
            $scope.userForm1.firstName1.$setViewValue('name1');
            $scope.userForm1.lastName1.$setViewValue('name2');
            $scope.$digest();
            expect($scope.userForm1.firstName1.$valid).toBe(false);
            expect($scope.userForm1.$invalid).toBe(true)
            expect($scope.userForm1.validateForm()).toBe(false);
            $scope.$digest();
            expect(angular.element(firstName1El(formEl)).attr('uib-popover')).toBe('This is invalid');
            return expectFirstName1FormGroupHasErrorClass(formEl).toBe(true);
        });
        
        it('Customer validator: Names different, so should be invalid', function() {
            var formEl;
            formEl = customValidatorCompileEl();
            $scope.userForm2.firstName2.$setViewValue('name1');
            $scope.userForm2.lastName2.$setViewValue('name2');
            $scope.$digest();
            expect($scope.userForm2.firstName2.$valid).toBe(false);
            expect($scope.userForm2.$invalid).toBe(true)
            expect($scope.userForm2.validateForm()).toBe(false);
            $scope.$digest();
            expect(angular.element(firstName2El(formEl)).attr('uib-popover')).toBe('Custom invalid');
            return expectFirstName2FormGroupHasErrorClass(formEl).toBe(true);
        });
        
        it('Customer validator: Names same, so should be valid', function() {
            var formEl;
            formEl = customValidatorCompileEl();
            $scope.userForm2.firstName2.$setViewValue('name');
            $scope.userForm2.lastName2.$setViewValue('name');
            $scope.$digest();
            expect($scope.userForm2.firstName2.$valid).toBe(true);
            expect($scope.userForm2.$invalid).toBe(false)
            return expect($scope.userForm2.validateForm()).toBe(true);
        });
    });
    
    
    var find = function(el, selector) {
        return el[0].querySelector(selector);
    };

    var firstName1El = function(el) {
        return find(el, '[name=firstName1]');
    };
    
    var firstName2El = function(el) {
        return find(el, '[name=firstName2]');
    };
    
    var expectFirstName1FormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=first-name-group1]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };
    
    var expectFirstName2FormGroupHasErrorClass = function(el) {
        var formGroup;
        formGroup = el[0].querySelector('[id=first-name-group2]');
        return expect(angular.element(formGroup).hasClass('has-error'));
    };
});