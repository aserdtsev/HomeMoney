'use strict';

hmControllers.controller('UserCtrl', ['$scope', '$rootScope', '$cookies', '$base64', '$http', 'UserSvc', UserCtrl]);
function UserCtrl($scope, $rootScope, $cookies, $base64, $http, UserSvc) {
  $rootScope.isLogged = false;
  $scope.email = $cookies.get('email');

  var response = UserSvc.getBalanceSheetId(function() {
    if (response.status === 'OK') {
      $rootScope.bsId = response.data;
      $rootScope.isLogged = true;
      $rootScope.$broadcast('login', $rootScope.bsId);
    }
  });

  $scope.login = function(email, pwd) {
    $http.defaults.headers.common.Authorization = 'Basic ' + $base64.encode(email + ":" + pwd);
    const response = UserSvc.login(function() {
      if (typeof response !== 'unassigned') {
        if (response.status === 'OK') {
          $rootScope.bsId = response.data['bsId'];
          $http.defaults.headers.common['X-Balance-Sheet-Id'] = $rootScope.bsId;
          $rootScope.isLogged = true;
          $scope.errMsg = '';
          const expireDate = new Date();
          expireDate.setDate(expireDate.getDate() + 3);
          $cookies.put('email', email, {expires: expireDate});
          $cookies.put('userId', response.data['userId'], {expires: expireDate});
          $rootScope.$broadcast('login');
        } else if (status === 'AuthWrong') {
          $rootScope.isLogged = false;
          $rootScope.bsId = undefined;
          $http.defaults.headers.common['X-Balance-Sheet-Id'] = undefined;
          $scope.errMsg = 'Неверный Email или пароль.';
        }
      }
    });
  };

  $scope.logout = function() {
    $http.defaults.headers.common.Authorization = 'Basic ';
    $rootScope.$broadcast('logout');
  }

  $scope.$on('logout', function() {
    $rootScope.isLogged = false;
    $rootScope.bsId = undefined;
    $rootScope.currencies = undefined;
    $cookies.remove('userId');
  });

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }
}
