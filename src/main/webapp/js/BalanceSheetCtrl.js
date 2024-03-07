hmControllers.controller('BalanceSheetCtrl', ['$scope', '$rootScope', 'BalanceSheetSvc', BalanceSheetCtrl]);
function BalanceSheetCtrl($scope, $rootScope, BalanceSheetSvc) {
  $scope.interval = 1;
  $scope.bsStat;

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.loadBsStat = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = BalanceSheetSvc.query({interval: $scope.interval}, function() {
      if (response.status == "OK") {
        $scope.bsStat = response.data;
      } else if (response.status == "INCORRECT_AUTH_TOKEN") {
        $rootScope.$broadcast('logout');
      }
    });
  };

  $scope.$on('login', function() {
    $scope.loadBsStat();
  });

  $scope.$on('logout', function(event) {
    $scope.bsStat = undefined;
  });

  $scope.$on('refreshBalanceSheet', function() {
    $scope.loadBsStat();
  });

  $scope.formatMoney = function(amount, currencySymbol, spaceChar) {
    return $rootScope.formatMoney(amount, currencySymbol, spaceChar);
  };

  $scope.options = {
    axes: {
      x: {key: 'date', type: 'date', ticks: 5},
      y: {type: 'linear'}
    },
    series: [
      {y: 'freeAmount', label: 'Свободные деньги', color: 'blue', thickness: '2px', type: 'area'},
      {y: 'incomeAmount', label: 'Доходы', color: 'green', type: 'column'},
      {y: 'chargeAmount', label: 'Расходы', color: 'orange', type: 'column'},
      {y: 'reserveSaldo', label: 'Резервы', thickness: '2px', type: 'area', visible: false},
      {y: 'totalSaldo', label: 'Баланс', color: 'steelblue', thickness: '2px', type: 'area', visible: false}
    ],
    lineMode: 'linear',
    tension: 0.7,
    tooltip: {mode: 'scrubber', formatter: function(x, y) {return $scope.formatMoney(y, '', ' ');}},
    drawLegend: true,
    drawDots: true,
    columnsHGap: 5
  }
}
