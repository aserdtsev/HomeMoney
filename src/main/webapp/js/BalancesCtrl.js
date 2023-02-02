'use strict';

hmControllers.controller('BalancesCtrl',
    ['$scope', '$rootScope', 'BalancesSvc', 'ReservesSvc', BalancesCtrl]);
function BalancesCtrl($scope, $rootScope, BalancesSvc, ReservesSvc) {
  $scope.balances;
  $scope.reserves;

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.getBalances = function(includeArc) {
    if (typeof $scope.balances == 'undefined') {
      return [];
    }
    if (includeArc) {
      return $scope.balances;
    } else {
      return $scope.balances.filter(function(balance) {
        return !balance['isArc'] || balance.isEdited;
      });
    }
  }

  $scope.getReserves = function() {
    if (typeof $scope.reserves == 'undefined') {
      return [];
    }
    return $scope.reserves.filter(function(reserve) {
      return !reserve['isArc'];
    });
  }

  $scope.loadBalances = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = BalancesSvc.query(function() {
      $scope.balances = response.data;
    });
  };

  $scope.loadReserves = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = ReservesSvc.query(function() {
      var list = response.data;
      list.splice(0, 0, {id: null, name: '<без резерва>'});
      $scope.reserves = list;
    });
  };

  $scope.refresh = function() {
    $scope.loadBalances();
    $scope.loadReserves();
  }

  $scope.$on('login', function() {
    $scope.refresh();
  });

  $scope.$on('logout', function() {
    $scope.balances = undefined;
    $scope.reserves = undefined;
  });

  $scope.$on('refreshBalanceSheet', function() {
    $scope.refresh();
  });

  $scope.$on('refreshAccounts', function() {
    $scope.refresh();
  });

  $scope.newBalance = function() {
    var balance = {id: null, createdDate: formatDate(new Date()), isArc: false, isEdited: true};
    $scope.balances.splice(0, 0, balance);
  };

  $scope.saveBalance = function(balance) {
    delete balance.isEdited;
    if (balance.id == null) {
      $scope.createBalance(balance);
    } else {
      $scope.updateBalance(balance);
    }
  };

  $scope.cancelEditBalance = function() {
    $rootScope.$broadcast('refreshAccounts');
  };

  $scope.createBalance = function(balance) {
    balance.id = randomUUID();
    BalancesSvc.create(balance, function() {
      $rootScope.$broadcast('refreshBalanceSheet');
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.updateBalance = function(balance) {
    BalancesSvc.update(balance, function() {
      $rootScope.$broadcast('refreshAccounts');
      $rootScope.$broadcast('refreshMoneyOpers');
    });
  };

  $scope.deleteBalance = function(balance) {
    BalancesSvc.delete(balance, function() {
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.upBalance = function(balance) {
    BalancesSvc.up(balance, function() {
      $rootScope.$broadcast('refreshAccounts');
    });
  }

  $scope.getReserveName = function(reserveId) {
    if (typeof $scope.reserves != 'undefined') {
      var result = $scope.reserves.filter(function (item) {
        return (item.id == reserveId)
      });
      if (result.length > 0) {
        return result[0]['name'];
      }
    }
    return 'n/a';
  };

  $scope.formatMoney = function(amount, currencySymbol) {
    return $rootScope.formatMoney(amount, currencySymbol);
  };
}

