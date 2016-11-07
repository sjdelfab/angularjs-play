// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
  'use strict';

  // -- RequireJS config --
  requirejs.config({
    // Packages = top-level folders; loads a contained file named 'main.js"
    packages: ['common', 'user', 'home'],
    shim: {
      'jsRoutes': {
         deps: [],
         // it's not a RequireJS module, so we have to tell it what var is returned
         exports: 'jsRoutes'
      },
      // Hopefully this all will not be necessary but can be fetched from WebJars in the future
      'angular': {
        deps: ['jquery'],
        exports: 'angular'
      },
      'angular-route': ['angular'],
      'angular-cookies': ['angular'],
      'angular-sanitize': ['angular'],
      'bootstrap': ['jquery'],
      'ui-bootstrap': ['angular'],
      'angular-ui-select': ['angular'],
      'angular-translate': ['angular'],
      'angular-translate-loader-partial': ['angular-translate'],
      'angular-translate-loader-url': ['angular-translate'],
      'satellizer': ['angular']
    },
    paths: {
      'requirejs': ['../lib/requirejs/require'],
      'jquery': ['../lib/jquery/jquery'],
      'angular': ['../lib/angularjs/angular'],
      'angular-route': ['../lib/angularjs/angular-route'],
      'angular-cookies': ['../lib/angularjs/angular-cookies'],
      'angular-sanitize': ['../lib/angularjs/angular-sanitize'],
      'bootstrap': ['../lib/bootstrap/js/bootstrap'],
      'ui-bootstrap': ['../lib/angular-ui-bootstrap/ui-bootstrap-tpls'],
      'angular-ui-select': ['../lib/angular-ui-select/select'],
      'angular-translate': ['../lib/angular-translate/angular-translate'],
      'angular-translate-loader-partial': ['../lib/angular-translate-loader-partial/angular-translate-loader-partial'],
      'angular-translate-loader-url': ['../lib/angular-translate-loader-url/angular-translate-loader-url'],
      'moment': ['../lib/momentjs/moment'],
      'jsRoutes': ['/jsroutes'],
      'satellizer': ['../lib/satellizer/satellizer']
    },
    config: {
        moment: {
            noGlobal: true
        }
    },
    urlArgs: "build=@Revision@"
  });

  requirejs.onError = function (err) {
    console.log(err);
  };

  // Load the app. This is kept minimal so it doesn't need much updating.
  require(['angular', 'angular-cookies', 'angular-route', 'angular-sanitize','jquery', 'bootstrap', 
           './app','ui-bootstrap','angular-ui-select','angular-translate', 'angular-translate-loader-url', 
           'angular-translate-loader-partial','satellizer'],
    function (angular) {
      angular.bootstrap(document, ['app']);
    }
  );
})(requirejs);
