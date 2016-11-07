// Karma configuration
// Generated on Sun Aug 24 2014 13:01:07 GMT+0930 (CST)

var tests = Object.keys(window.__karma__.files).filter(function (file) {
    return (/Spec\.js$/).test(file);
});

requirejs.config({
	packages: ['common','resource','home','user'],
    paths: {
        // External libraries
        'angular': '/base/target/web/web-modules/main/webjars/lib/angularjs/angular',
        'angularMocks': '/base/target/web/web-modules/main/webjars/lib/angularjs/angular-mocks',
        'jquery': '/base/target/web/web-modules/main/webjars/lib/jquery/jquery',
        'angular-route': '/base/target/web/web-modules/main/webjars/lib/angularjs/angular-route',
        'angular-cookies': '/base/target/web/web-modules/main/webjars/lib/angularjs/angular-cookies',
        'angular-sanitize': '/base/target/web/web-modules/main/webjars/lib/angularjs/angular-sanitize',
        'bootstrap': '/base/target/web/web-modules/main/webjars/lib/bootstrap/js/bootstrap',
        'ui-bootstrap': '/base/target/web/web-modules/main/webjars/lib/angular-ui-bootstrap/ui-bootstrap-tpls',
        'moment': '/base/target/web/web-modules/main/webjars/lib/momentjs/moment',
        'angular-ui-select': '/base/target/web/web-modules/main/webjars/lib/angular-ui-select/select',
        'angular-translate': ['/base/target/web/web-modules/main/webjars/lib/angular-translate/angular-translate'],
        'angular-translate-loader-partial': ['/base/target/web/web-modules/main/webjars/lib/angular-translate-loader-partial/angular-translate-loader-partial'],
        'angular-translate-loader-url': ['/base/target/web/web-modules/main/webjars/lib/angular-translate-loader-url/angular-translate-loader-url'],
        'satellizer': ['/base/target/web/web-modules/main/webjars/lib/satellizer/satellizer'],
        'app': './app'
    },

    baseUrl: 'base/app/assets/javascripts',

    shim: {
        'angular': {
            deps: ['jquery'],
            exports: 'angular'
          },          
        'angularMocks': {deps: ['angular'], 'exports': 'angular.mock'},
        'angular-route': ['angular'],
        'angular-cookies': ['angular'],
        'angular-sanitize': ['angular'],
        'angular-ui-select': ['angular'],
        'bootstrap': ['jquery'],
        'ui-bootstrap': ['angular'],
        'angular-translate': ['angular'],
        'angular-translate-loader-partial': ['angular-translate'],
        'angular-translate-loader-url': ['angular-translate'],
        'satellizer': ['angular']
    },

    // Ask Require.js to load these files (all our tests).
    deps: tests,

    // Set test to start run once Require.js is done.
    callback: window.__karma__.start
});
