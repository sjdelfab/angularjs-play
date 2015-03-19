/**
 * 
 */
define([ 'angular','moment' ], function(angular,moment) {
	'use strict';

	var mod = angular.module('common.directives.bs-datefield', []);
	mod.directive('bsDatefield', function() {
		return {
			require : 'ngModel',
			link : function(scope, element, attrs, ngModelCtrl) {
				var dateFormat = attrs.bsDatefield || 'DD/MM/YYYY'; // default en-AU,GB
				/* jshint unused:false */
				ngModelCtrl.$parsers.push(function(viewValue) {
					var newDate = ngModelCtrl.$viewValue;

					// pass through if we clicked date from popup
					if (typeof newDate === "object" || newDate === "") {
						if (attrs.min !== undefined) {
							if (moment(attrs.min).isAfter(newDate)) {
								ngModelCtrl.$setValidity('min', false);
							} else {
								ngModelCtrl.$setValidity('min', true);
							}
						}
						if (attrs.maxDate !== undefined) {
							if (moment(attrs.maxDate).isBefore(newDate)) {
								ngModelCtrl.$setValidity('max', false);
							} else {
								ngModelCtrl.$setValidity('max', true);
							}
						}
						return newDate;
					}

					var parsedMoment = moment(newDate, dateFormat);
					if (parsedMoment.isValid()) {
						if (attrs.min !== undefined) {
							if (parsedMoment.isBefore(attrs.min)) {
								ngModelCtrl.$setValidity('min', false);
								ngModelCtrl.$setValidity('required', false);
								return parsedMoment.toDate();
							} else {
								ngModelCtrl.$setValidity('min', true);
							}
						}
						if (attrs.maxDate !== undefined) {
							if (parsedMoment.isAfter(attrs.maxDate)) {
								ngModelCtrl.$setValidity('max', false);
								ngModelCtrl.$setValidity('required', false);
								return parsedMoment.toDate();
							} else {
								ngModelCtrl.$setValidity('max', true);
							}
						}
						ngModelCtrl.$setValidity('date', true);
						ngModelCtrl.$setValidity('required', true);
						return parsedMoment.toDate();
					} else {
						ngModelCtrl.$setValidity('date', false);
						ngModelCtrl.$setValidity('required', false);
						return undefined;
					}

				});

				ngModelCtrl.$formatters.push(function(modelValue) {
					var isModelADate = angular.isDate(modelValue);
					ngModelCtrl.$setValidity('date', isModelADate);
					return isModelADate ? moment(modelValue).format(dateFormat) : undefined;
				});

				element.on('keydown', {
					scope : scope,
					datePickerPopupOpen : attrs.isOpen
				}, function(e) {
					var response = true;
					// the scope of the date control
					var scope = e.data.scope;
					// the variable name for the open state of the popup (also controls it!)
					var openId = e.data.datePickerPopupOpen;

					switch (e.keyCode) {
					case 13: // ENTER
						scope[openId] = !scope[openId];
						// update manually view
						if (!scope.$$phase) {
							scope.$apply();
						}
							
						response = false;
						break;

					case 9: // TAB
						scope[openId] = false;
						// update manually view
						if (!scope.$$phase) {
							scope.$apply();
						}
						break;
					}

					return response;
				});
			}
		};
	});
	return mod;
});