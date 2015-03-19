/**
 * Running unit tests:
 * 
 * Obviously need to have NodeJS installed.
 * Install Grunt with:
 * npm install -g grunt-cli
 * Install the projects packages with
 * npm install
 * 
 */

module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);
	
  grunt.initConfig({
      pkg: grunt.file.readJSON('package.json'),
      karma: {
          options: {
              basePath: '.',
              frameworks: ['jasmine', 'requirejs'],
              //logLevel: 'LOG_DEBUG',
              files: [
                  {
                      pattern: 'app/assets/javascripts/**/*.js',
                      included: false
                  }, 
                  {
                      pattern: 'test/unit/*.js',
                      included: false
                  },
                  {
                      pattern: 'app/assets/javascripts/**/*.js.map',
                      included: false
                  },
                  {
                      pattern: 'target/web/web-modules/main/webjars/**/*.js',
                      included: false
                  },                  

                  'karma.conf.js'
              ],

              exclude: [
                  'app/javascripts/**/main.js'
              ]
          },
          dist: {
              singleRun: true,
              browsers: ['PhantomJS'],

              preprocessors: {
                  'app/javascripts/**/*.js': ['coverage']
              },

              reporters: ['dots', 'junit', 'coverage'],

              coverageReporter: {
                  type : 'cobertura',
                  dir: 'log/coverage/'
              },

              junitReporter: {
                  outputFile: 'log/test-results.xml'
              }
          },
          dev: {
              autoWatch: true,
              browsers: ['Chrome']
          }
      }
  });
  
  grunt.registerTask('default',['karma:dev']);

};
