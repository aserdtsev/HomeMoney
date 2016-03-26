'use strict';

hmControllers.controller('ReservesCtrl',
    ['$scope', '$rootScope', 'ReservesSvc', ReservesCtrl]);
function ReservesCtrl($scope, $rootScope, ReservesSvc) {
  $scope.reserves;

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.getReserves = function(includeArc) {
    if (typeof $scope.reserves == 'undefined') {
      return [];
    }
    if (includeArc) {
      return $scope.reserves;
    } else {
      return $scope.reserves.filter(function(reserve) {
        return !reserve['isArc'] || reserve.isEdited;
      });
    }
  }

  $scope.loadReserves = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = ReservesSvc.query({bsId: $rootScope.bsId}, function() {
      $scope.reserves = response.data;
    });
  };

  $scope.refresh = function() {
    $scope.loadReserves();
  }

  $scope.$on('login', function() {
    $scope.loadReserves();
  });

  $scope.$on('logout', function() {
    $scope.reserves = undefined;
  });

  $scope.$on('refreshBalanceSheet', function() {
    $scope.refresh();
  });

  $scope.newReserve = function() {
    var reserve = {id: null, isArc: false, isEdited: true};
    $scope.reserves.splice(0, 0, reserve);
  };

  $scope.saveReserve = function(reserve) {
    delete reserve.isEdited;
    if (reserve.id == null) {
      $scope.createReserve(reserve);
    } else {
      $scope.updateReserve(reserve);
    }
  };

  $scope.cancelEditReserve = function() {
    $scope.refresh();
  };

  $scope.createReserve = function(reserve) {
    reserve.id = randomUUID();
    ReservesSvc.create({bsId: $rootScope.bsId}, reserve, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshBalanceSheet');
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.updateReserve = function(reserve) {
    ReservesSvc.update({bsId: $rootScope.bsId}, reserve, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.deleteReserve = function(reserve) {
    ReservesSvc.delete({bsId: $rootScope.bsId}, reserve, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.formatMoney = function(amount, currencySymbol) {
    return $rootScope.formatMoney(amount, currencySymbol);
  }
}

