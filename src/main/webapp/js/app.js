                                                           'use strict';

var hmApp = angular.module('hmApp', [
  'ngRoute',
  'ngSanitize',
  'ngCookies',
  'n3-line-chart',
  'hmControllers',
  'hmServices'
]);

hmApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
        when('/index', {
          templateUrl: 'index.html',
          controller: 'UserCtrl'
        }).
        otherwise({
          redirectTo: '/index'
        });
  }]);

function randomUUID() {
  function S4() {
    return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
  }
  return (S4() + S4() + "-" + S4() + "-4" + S4().substr(0,3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
}

function formatDate(date) {
  var d = date.getDate()
  if ( d < 10 ) d = '0' + d;
  var m = date.getMonth()+1
  if ( m < 10 ) m = '0' + m;
  var y = date.getFullYear();
  return y + '-' + m + '-' + d;
}
