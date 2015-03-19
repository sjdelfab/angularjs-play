define(['angular'], function(angular) {
	'use strict';

	/**
	 * https://raw.githubusercontent.com/angular-ui/ui-utils/master/modules/validate/validate.js
	 * 
	 * General-purpose validator for ngModel. angular.js comes with several
	 * built-in validation mechanism for input fields (ngRequired, ngPattern
	 * etc.) but using an arbitrary validation function requires creation of a
	 * custom formatters and / or parsers. The ui-validate directive makes it
	 * easy to use any function(s) defined in scope as a validator function(s).
	 * A validator function will trigger validation on both model and input
	 * changes.
	 * 
	 * @example <input ui-validate=" 'myValidatorFunction($value)' ">
	 * @example <input ui-validate="{ foo : '$value > anotherModel', bar : 'validateFoo($value)' }">
	 * @example <input ui-validate="{ foo : '$value > anotherModel' }" ui-validate-watch=" 'anotherModel' ">
	 * @example <input ui-validate="{ foo : '$value > anotherModel', bar : 'validateFoo($value)' }" ui-validate-watch=" { foo : 'anotherModel' } ">
	 * 
	 * @param ui-validate
	 *            {string|object literal} If strings is passed it should be a
	 *            scope's function to be used as a validator. If an object
	 *            literal is passed a key denotes a validation error key while a
	 *            value should be a validator function. In both cases validator
	 *            function should take a value to validate as its argument and
	 *            should return true/false indicating a validation result.
	 */
	angular.module('ui.validate', []).directive('uiValidate',['$compile', function($compile) {

		return {
			restrict : 'A',
			require : 'ngModel',
			link : function(scope, elm, attrs, ctrl) {
				var validateFn, validators = {}, validateExpr = scope.$eval(attrs.uiValidate);

				if (!validateExpr) {
					return;
				}

				if (angular.isString(validateExpr)) {
					validateExpr = {
						validator : validateExpr
					};
				}

				angular.forEach(validateExpr, function(exprssn, key) {
					validateFn = function(valueToValidate) {
						var expression = scope.$eval(exprssn, {
							'$value' : valueToValidate
						});
						if (angular.isObject(expression) && angular.isFunction(expression.then)) {
							// expression is a promise
							expression.then(function() {
								ctrl.$setValidity(key, true);
							}, function() {
								ctrl.$setValidity(key, false);
							});
							return valueToValidate;
						} else if (expression) {
							// expression is true
							ctrl.$setValidity(key, true);
							return valueToValidate;
						} else {
							// expression is false
							ctrl.$setValidity(key, false);
							return valueToValidate;
						}
					};
					validators[key] = validateFn;
					ctrl.$formatters.push(validateFn);
					ctrl.$parsers.push(validateFn);
				});

				function apply_watch(watch) {
					// string - update all validators on expression change
					if (angular.isString(watch)) {
						scope.$watch(watch, function() {
							angular.forEach(validators, function(validatorFn) {
								validatorFn(ctrl.$modelValue);
							});
						});
						return;
					}

					// array - update all validators on change of any expression
					if (angular.isArray(watch)) {
						angular.forEach(watch, function(expression) {
							scope.$watch(expression, function() {
								angular.forEach(validators, function(validatorFn) {
									validatorFn(ctrl.$modelValue);
								});
							});
						});
						return;
					}

					// object - update appropriate validator
					if (angular.isObject(watch)) {
						angular.forEach(watch, function(expression, validatorKey) {
							// value is string - look after one expression
							if (angular.isString(expression)) {
								scope.$watch(expression, function() {
									validators[validatorKey](ctrl.$modelValue);
								});
							}

							// value is array - look after all expressions in
							// array
							if (angular.isArray(expression)) {
								angular.forEach(expression, function(intExpression) {
									scope.$watch(intExpression, function() {
										validators[validatorKey](ctrl.$modelValue);
									});
								});
							}
						});
					}
				}
				// Support for ui-validate-watch
				if (attrs.uiValidateWatch) {
					apply_watch(scope.$eval(attrs.uiValidateWatch));
				}
			}
		};
	}]).directive("validatePopups", ['$compile',function($compile) {
		// http://www.bennadel.com/blog/2651-looking-at-compile-and-maxpriority-in-angularjs.htm
		return {
			priority : 1500, // compiles first
			restrict : 'A',
			terminal : true, // prevent lower priority directives to compile after it
			compile : function(el) {
				// parent form
				var form = el.parents("form");
                if (form === null || angular.isUndefined(form) || angular.isUndefined(form[0])) {
					throw new Error("Must have an outer form");
				}
				var formName = form[0].name;
				if (formName === null || angular.isUndefined(formName) || formName === '') {
					throw new Error("Outer form must have a name");
				}
				// necessary to avoid infinite compile loop
				el.removeAttr('popover-placement'); 
				el.removeAttr('popover-trigger');
				el.removeAttr('popover');

				// Add the popover attributes
				el.attr('popover-placement', 'right');
				el.attr('popover-trigger', 'focus');
				var elementName = el[0].name;
				if (elementName === null || angular.isUndefined(elementName) || elementName === '') {
					throw new Error("Element must have a name");
				}
				if (angular.isUndefined(form.attr('data-form-validation'))) {
					throw new Error("Outer form must have form-validation attribute");
				}
				var validationObjPath = '{{' + formName + '.validationMessages.' + elementName + '}}';
				el.attr('popover', validationObjPath);
				var fn = $compile(el, null, 1500);
				return function(scope) {
					fn(scope);
				};
			}
		};
	}]).directive("formValidation", function() {
		// inspired by http://blog.yodersolutions.com/bootstrap-form-validation-done-right-in-angularjs/
		return {
			restrict : 'A',
			link : function(scope, el, attrs, formCtrl) {
			    var formName = el[0].name;
			    formCtrl = dotEval(scope,formName); 
			    formCtrl.validationMessages = {};
			    formCtrl.validateForm = function() {
					var valid = true;
					// find the text box element, which has the 'name' attribute
					var formGroups = el[0].querySelectorAll("div.form-group");
					angular.forEach(formGroups, function(formGroup) {
						var jqFormGroup = angular.element(formGroup);						
						// There could be multiple input fields
						angular.forEach(formGroup.querySelectorAll("input"), function(inputField) {
							// convert the native text box element to an angular element
							var inputNgEl = angular.element(inputField);
							// get the name on the text box so we know the property to check on the form controller
							var inputName = inputNgEl.attr('name');
							if (formCtrl[inputName].$invalid) {
								valid = false;
								jqFormGroup.toggleClass('has-error', true);
								formCtrl.validationMessages[inputName] = getErrorMessage(formCtrl,inputName,inputNgEl);
							} else {
							    formCtrl.validationMessages[inputName] = '';
								jqFormGroup.toggleClass('has-error', false);
							}
						});
					});
					return valid;
				};
				
				function dotEval(obj, prop, val) {
				    var props = prop.split('.'), final = props.pop(), p = props.shift(); 
				    while (p) {
				        if (typeof obj[p] === 'undefined') {
				            return undefined;
				        }
				        obj = obj[p];
				        p = props.shift();
				    }
				    return val ? (obj[final] = val) : obj[final];
				}
				
				function getErrorMessage(formCtrl,inputName,inputNgEl) {
				    var errorMessage = "Unknown";
				    var inputObject = dotEval(formCtrl,inputName); 
                    if (inputObject.$error.required) {
                        errorMessage = "This is a required field";
                    } else if (inputObject.$error.email) {
                        errorMessage = "Invalid email address";
                    } else if (inputObject.$error.maxlength) {
                        var maxLength = inputNgEl.attr('data-ng-maxlength');
                        errorMessage = "Maximum length is " + maxLength + " characters";
                    } else if (inputObject.$error.minlength) {
                        var minLength = inputNgEl.attr('data-ng-minlength');
                        errorMessage = "Minimum length is " + minLength + " characters";
                    } else if (inputObject.$error.pattern) {
                        var invalidPatternMessage = inputNgEl.attr('data-invalid-pattern-message');
                        if (invalidPatternMessage === null || angular.isUndefined(invalidPatternMessage) || invalidPatternMessage === '') {
                            throw new Error("Must specify an invalid pattern message");
                        }
                        errorMessage = invalidPatternMessage;
                    } else if (hasUiValidateElement(inputNgEl)) {
                        var validateExpr = scope.$eval(inputNgEl.attr('ui-validate'));
                        if (!validateExpr) {
                            throw new Error("Need a valid expression");
                        }
                        var customValidationMessage;
                        if (angular.isString(validateExpr)) {
                            // look for ui-validate-message attribute
                            customValidationMessage = inputNgEl.attr('ui-validate-message');
                        } else {
                            // custom expression, so iterate over each and find out they are valid; if so, get first message
                            for(var customValidatorName in validateExpr) {
                               if (inputObject.$error[customValidatorName]) {
                                   var denormalisedName = customValidatorName.replace(/([A-Z])/g, '-$1').toLowerCase();
                                   var possibleErrorMessage = inputNgEl.attr('ui-validate-' + denormalisedName);
                                   if (possibleErrorMessage !== null && angular.isDefined(possibleErrorMessage)) {
                                       customValidationMessage = possibleErrorMessage;
                                   }
                               }
                            }
                        }
                        if (customValidationMessage === null || angular.isUndefined(customValidationMessage) || customValidationMessage === '') {
                            throw new Error("Must specify a custom validation message");
                        }
                        errorMessage = customValidationMessage;
                    }
                    return errorMessage;
				}
				
				function hasUiValidateElement(inputNgEl) {
				    var attr = inputNgEl.attr('ui-validate');
				    return attr !== null && angular.isDefined(attr);
				}
				
			}
		};
	});
});