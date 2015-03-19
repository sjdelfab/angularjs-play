/**
 * Useful Calendar functions and data. 
 */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.calendar-util', []);
  mod.service('CalenderUtil', function() {
	
     var ReferenceData = {};

     ReferenceData.months = [{month: 0, name: 'Jan'},
                             {month: 1, name: 'Feb'},
                             {month: 2, name: 'Mar'},
                             {month: 3, name: 'Apr'},
                             {month: 4, name: 'May'},
                             {month: 5, name: 'Jun'},
                             {month: 6, name: 'Jul'},
                             {month: 7, name: 'Aug'},
                             {month: 8, name: 'Sep'},
                             {month: 9, name: 'Oct'},
                             {month: 10, name: 'Nov'},
                             {month: 11, name: 'Dec'}];

	ReferenceData.weekdays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];  
  
    return {
      getMonths: function() {
          return ReferenceData.months;
      },
      getWeekdays: function() {
          return ReferenceData.weekdays;
      },
      isWeekend: function(dayOfWeek) {
          if (ReferenceData.weekdays[dayOfWeek] == 'Sun' || ReferenceData.weekdays[dayOfWeek] == 'Sat') {
             return true;
          }
          return false;
      },
      isSaturday: function(dayOfWeek) {
          if (ReferenceData.weekdays[dayOfWeek] == 'Sat') {
             return true;
          }
          return false;
      },
      isSunday: function(dayOfWeek) {
          if (ReferenceData.weekdays[dayOfWeek] == 'Sun') {
             return true;
          }
          return false;
      },
      isFriday: function(dayOfWeek) {
          if (ReferenceData.weekdays[dayOfWeek] == 'Fri') {
             return true;
          }
          return false;
      }
    };
  });
  return mod;
});