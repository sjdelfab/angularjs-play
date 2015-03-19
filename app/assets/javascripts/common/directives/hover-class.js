/**
 * 
 */
define([ 'angular' ], function(angular) {
	'use strict';

	var mod = angular.module('common.directives.hover-class', []);
	mod.directive('hoverClass', function() {
		return {
			restrict : 'A',
			scope : {
				hoverClass : '@'
			},
			link : function(scope, element) {
				element.on('mouseenter', function() {
					element.addClass(scope.hoverClass);
					scope.$apply();
				});
				element.on('mouseleave', function() {
					element.removeClass(scope.hoverClass);
					scope.$apply();
				});
			}
		};
	});
	return mod;
});