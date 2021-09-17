'use strict';

hmControllers.controller('InitCtrl',
    ['$scope', '$rootScope', 'ReferencesSvc', InitCtrl]);
function InitCtrl($scope, $rootScope, ReferencesSvc) {
  $scope.$on('login', function() {
    $scope.loadCurrencies();
  });

  $scope.loadCurrencies = function() {
    if ($rootScope.isLogged) {
      var response = ReferencesSvc.currencies(function () {
        $rootScope.currencies = response.data;
      });
    }
  }

  $rootScope.getCurrencySymbol = function(currencyCode) {
    return $rootScope.currencies.filter(function(item) {
      return item['currencyCode'] == currencyCode;
    })[0]['symbol'];
  }

  $rootScope.formatMoney = function(amount, currencySymbol, spaceChar){
    if (typeof amount == 'undefined') {
      return null;
    }
    if (typeof spaceChar == 'undefined') {
      spaceChar = '&nbsp;';
    }
    if (currencySymbol == undefined) {
      currencySymbol = '';
    }
    var amountStrBuf = parseFloat(amount).toFixed(2);
    var sign = '';
    if (amountStrBuf[0] == '-') {
      sign = '-';
      amountStrBuf = amountStrBuf.substr(1, amountStrBuf.length - 1);
    }
    var result = spaceChar + currencySymbol;
    var isFirstPass = true;
    while (amountStrBuf.length > 3) {
      result = (!isFirstPass ? spaceChar : '') + amountStrBuf.substr(amountStrBuf.length - 3, 3) + result;
      amountStrBuf = amountStrBuf.substr(0, amountStrBuf.length - 3);
      isFirstPass = false;
    }
    result = sign + amountStrBuf + result;
    return result;
  };

}
