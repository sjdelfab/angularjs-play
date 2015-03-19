define(['angular', './services/calendar-util', './services/http-handlers', './services/play-routes',
        './directives/angular-multi-select', './directives/bs-datefield', './directives/draganddrop', 
        './directives/hover-class','./directives/validate','./services/angular-block-ui',
        './services/message-dialog','./filters'],
    function(angular) {
  'use strict';

  return angular.module('myapp.common', ['multi-select', 'common.directives.bs-datefield', 'ang-drag-drop', 
                                         'common.directives.hover-class', 'common.calendar-util', 'common.playRoutes',
                                         'common.http-handler','ui.validate','blockUI','common.message-dialog',
                                         'common.filters']);
});
