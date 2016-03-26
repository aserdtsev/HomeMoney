'use strict';

hmControllers.controller('UserCtrl', ['$scope', '$rootScope', '$cookies', 'UserSvc', 'ReferencesSvc', UserCtrl]);
function UserCtrl($scope, $rootScope, $cookies, UserSvc, ReferencesSvc) {
  $rootScope.isLogged = false;
  $scope.email = $cookies.get('email');

  var response = UserSvc.getBalanceSheetId(function() {
    if (response.status == 'OK') {
      $rootScope.bsId = response.data;
      $rootScope.isLogged = true;
      $rootScope.$broadcast('login', $rootScope.bsId);
    }
  });

  $scope.login = function(email, pwd) {
    var response = UserSvc.login({email: email, pwd: pwd}, function() {
      if (typeof response != 'unassigned') {
        if (response.status == 'OK') {
          $rootScope.bsId = response.data['bsId'];
          $rootScope.isLogged = true;
          $scope.errMsg = '';
          var expireDate = new Date();
          expireDate.setDate(expireDate.getDate() + 3);
          $cookies.put('email', email, {expires: expireDate});
          $cookies.put('userId', response.data['userId'], {expires: expireDate});
          $cookies.put('authToken', response.data['token']);
          $rootScope.$broadcast('login');
        } else if (status == 'AuthWrong') {
          $rootScope.isLogged = false;
          $rootScope.bsId = undefined;
          $scope.errMsg = 'Неверный Email или пароль.';
        }
      }
    });
  };

  $scope.logout = function() {
    UserSvc.logout(function() {
      $rootScope.$broadcast('logout');
    });
  }

  $scope.$on('logout', function() {
    $rootScope.isLogged = false;
    $rootScope.bsId = undefined;
    $rootScope.currencies = undefined;
    $cookies.remove('userId');
    $cookies.remove('authToken');
  });

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }
}
