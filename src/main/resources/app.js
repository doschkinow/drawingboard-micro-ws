'use strict';

angular.module('myApp', ['myApp.services']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/drawings/:drawingId', {templateUrl: 'drawing.html', controller: DrawingController});
  }]);
