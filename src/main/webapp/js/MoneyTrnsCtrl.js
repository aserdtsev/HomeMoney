'use strict';

hmControllers.controller('MoneyTrnsCtrl',
    ['$scope', '$rootScope', 'ReferencesSvc', 'AccountsSvc', 'MoneyTrnsSvc', 'MoneyTrnTemplsSvc', MoneyTrnsCtrl]);
function MoneyTrnsCtrl($scope, $rootScope, ReferencesSvc, AccountsSvc, MoneyTrnsSvc, MoneyTrnTemplsSvc) {
  $scope.defaultDate;
  $scope.pageSize = 5;
  $scope.accounts;
  $scope.search = '';
  $scope.trns;
  $scope.templs;

  $scope.$on('login', function() {
    $scope.loadTempls();
    $scope.loadTrnsFirstPage($scope.pageSize);
    $scope.loadAccounts();
  });

  $scope.$on('logout', function() {
    $scope.templs = undefined;
    $scope.accounts = undefined;
    $scope.fromAccounts = undefined;
    $scope.toAccounts = undefined;
    $scope.trns = undefined;
    $scope.search = undefined;
  });

  $scope.$on('refreshAccounts', function() {
    $scope.loadAccounts();
  });

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.find = function(search) {
    $scope.search = search;
    $scope.refresh();
  }

  $scope.refresh = function() {
    $scope.loadTempls();
    $scope.refreshTrns();
    $rootScope.$broadcast('refreshBalanceSheet');
  }

  $scope.refreshTrns = function() {
    $scope.loadTrnsFirstPage($scope.getTrnsLength());
  }

  $scope.loadAccounts = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = AccountsSvc.query({bsId: $rootScope.bsId}, function() {
      $scope.accounts = response.data.filter(function(account) {
        return !account['isArc'];
      });
    });
  };

  $scope.getGroupList = function(caption) {
    var result;
    $scope.trns.data.forEach(function(groupList) {
      if (typeof result == 'undefined' && groupList.caption == caption) {
        result = groupList;
      }
    });
    if (typeof result == 'undefined') {
      result = {caption: caption, isDone: caption != 'Новые' && caption != 'Ближайшие', expanded: true, items: []};
      if (caption == 'Ближайшие') {
        result.expanded = false;
      }
      $scope.trns.data.splice(0, 0, result);
      $scope.trns.data.sort(function(a, b) {
        if (a.caption < b.caption) return 1;
        if (a.caption > b.caption) return -1;
      });
    }
    return result;
  }

  $scope.addToTrns = function(list) {
    var today = $scope.getToday();
    list.forEach(function(trn) {
      trn.currencySymbol = $rootScope.getCurrencySymbol(trn['currencyCode']);
      var groupName = (trn.status == "done") ? trn.trnDate : "Ближайшие";
      var groupList = $scope.getGroupList(groupName);
      groupList.items = groupList.items.concat(trn);
      if (groupName == 'Ближайшие' && (new Date(trn.trnDate + 'Z+06:00') - today) == 0) {
        groupList.expanded = true;
      }
    });
  }

  $scope.loadTempls = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = MoneyTrnTemplsSvc.query({bsId: $rootScope.bsId, search: $scope.search}, function() {
      $scope.templs = response.data;
      $scope.templs.forEach(function(templ) {
        templ['currencySymbol'] = $rootScope.getCurrencySymbol(templ['currencyCode']);
      });
    });
  }

  $scope.loadTrnsFirstPage = function(limit) {
    if (!$scope.isLogged()) {
      return;
    }
    var qLimit = limit;
    if (typeof limit == 'undefined') {
      qLimit = $scope.pageSize;
    }
    var response = MoneyTrnsSvc.query({bsId: $rootScope.bsId, search: $scope.search, limit: qLimit, offset: 0}, function() {
      $scope.trns = {data: [], hasNext: response.data.paging.hasNext};
      $scope.addToTrns(response.data.items);
    });
  };

  $scope.loadTrnsNextPage = function(search) {
    var offset = $scope.getTrnsLength();
    var response = MoneyTrnsSvc.query({bsId: $rootScope.bsId, search: search, limit: $scope.pageSize, offset: offset}, function () {
      $scope.addToTrns(response.data.items);
      $scope.trns.hasNext = response.data.paging.hasNext;
    });
  };

  // Возвращает количество завершенных операций в $scope.trns.
  $scope.getTrnsLength = function() {
    var length = 0;
    $scope.trns.data.forEach(function(groupList) {
      length += groupList.isDone ? groupList.items.length : 0;
    });
    return length;
  }

  $scope.hasTrnsNextPage = function() {
    if (typeof $scope.trns == 'undefined' || typeof $scope.trns.data == 'undefined') {
      return false;
    }
    return $scope.trns.hasNext;
  };

  $scope.getDefaultDate = function() {
    if (typeof $scope.defaultDate == 'undefined') {
      $scope.defaultDate = $scope.getToday();
    }
    return $scope.defaultDate;
  };

  $scope.getToday = function() {
    var today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
  }

  $scope.getFirstAccounts = function(trn) {
    if (typeof $scope.accounts == 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      if (trn.type == 'transfer') {
        var result = account.type == 'debit' || account.type == 'credit'
            || account == 'asset' || account.type == 'reserve' || account.type == 'service';
        if (result && typeof trn.toAccId != 'undefined') {
          result = result && account.id != trn.toAccId;
          if (result) {
            var toAccount = $scope.getAccount(trn.toAccId);
            if (toAccount.type == 'reserve' || toAccount.type == 'service') {
              result = result && (account.type == 'reserve' || account.type == 'service');
            } else {
              result = result && account.type != 'reserve' && account.type != 'service';
            }
          }
        }
        return result;
      } else {
        return account.type == 'debit' || account.type == 'credit' || account.type == 'asset';
      }
    });
  }

  $scope.getSecondAccounts = function(trn) {
    if (typeof $scope.accounts == 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      if (trn.type == 'transfer') {
        var result = account.type == 'debit' || account.type == 'credit'
            || account == 'asset' || account.type == 'reserve';
        if (result && typeof trn.fromAccId != 'undefined') {
          result = result && account.id != trn.fromAccId;
          if (result) {
            var fromAccount = $scope.getAccount(trn.fromAccId);
            if (fromAccount.type == 'reserve' || fromAccount.type == 'service') {
              result = result && (account.type == 'reserve' || account.type == 'service');
            } else {
              result = result && account.type != 'reserve' && account.type != 'service';
            }
          }
        }
        return result;
      } else {
        return account.type == trn.type;
      }
    });
  }

  $scope.getAccount = function(id) {
    return $scope.accounts.filter(function(account) {
      return account.id == id;
    })[0];
  }

  $scope.newTrn = function(param) {
    var trn;
    if (typeof param.id != 'undefined') {
      var templ = param;
      trn = {type: templ.type, fromAccId: templ.fromAccId, amount: templ.amount, toAccId: templ.toAccId,
        comment: templ.comment, labels: templ.labels, period: templ.period, templId: templ.id};
    } else {
      var type = param;
      trn = {type: type, labels: []};
    }
    trn.status = 'done';
    trn.trnDate = formatDate($scope.getDefaultDate());
    trn.isEdited = true;
    var groupList = $scope.getGroupList('Новые');
    groupList.items.splice(0, 0, trn);
  };

  $scope.completeTrn = function(trn) {
    var today = formatDate(new Date());
    if (trn.trnDate > today) {
      trn.trnDate = today;
    }
    trn.status = 'done';
    trn.isEdited = true;
  }

  $scope.cancelTrn = function() {
    $scope.refreshTrns();
  };

  $scope.saveTrn = function(trn) {
    delete trn.isEdited;
    delete trn.currencySymbol;
    if (trn.id == null) {
      $scope.createTrn(trn);
    } else {
      $scope.updateTrn(trn);
    }
    $scope.defaultDate = new Date(trn.trnDate + 'Z+06:00');
  };

  $scope.createTrn = function(trn) {
    trn.id = randomUUID();
    var response = MoneyTrnsSvc.create({bsId: $rootScope.bsId}, trn, function() {
      $scope.loadTrnsFirstPage($scope.getTrnsLength() + response.data.length);
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.updateTrn = function(trn) {
    delete trn.currencySymbol;
    MoneyTrnsSvc.update({bsId: $rootScope.bsId}, trn, function() {
      $scope.loadTrnsFirstPage($scope.getTrnsLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.deleteTrn = function(trn) {
    delete trn.isEdited;
    delete trn.currencySymbol;
    MoneyTrnsSvc.delete({bsId: $rootScope.bsId}, trn, function() {
      $scope.loadTrnsFirstPage($scope.getTrnsLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.skipTrn = function(trn) {
    delete trn.currencySymbol;
    MoneyTrnsSvc.skip({bsId: $rootScope.bsId}, trn, function() {
      $scope.loadTrnsFirstPage($scope.getTrnsLength());
      $scope.loadTempls();
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  }

  $scope.upTrn = function(trn) {
    delete trn.isEdited;
    MoneyTrnsSvc.up({bsId: $rootScope.bsId}, trn, function() {
      $scope.loadTrnsFirstPage($scope.getTrnsLength());
    });
  };

  // item - экземпляр Trn или Templ.
  $scope.newLabelKeyPressed = function(event, label, item) {
    if (event.keyCode == 13) {
      var labels = item['labels'];
      if (typeof label != 'undefined' && label.length > 0) {
        labels.splice(labels.length, 0, label);
      }
    }
  };

  // item - экземпляр Trn или Templ.
  $scope.editLabelKeyPressed = function(event, index, label, item) {
    if (event.keyCode == 13) {
      var labels = item['labels'];
      if (typeof label != 'undefined' && label.length > 0) {
        labels[index] = label;
      } else {
        labels.splice(index, 1);
      }
    }
  };

  $scope.getSignedAmount = function(trn) {
    return trn['amount'] * (trn['type'] == 'e' ? -1 : 1);
  };

  $scope.getPeriodName = function(trn) {
    var period = trn['period'];
    return (period == 'month') ? 'Месяц' :
        (period == 'quarter') ? 'Квартал' :
        (period == 'year') ? 'Год' :
        (period == 'single') ? 'Разовая' : '?';
  }

  $scope.formatMoney = function(amount, currencySymbol) {
    return $rootScope.formatMoney(amount, currencySymbol);
  };

  $scope.showSearchClick = function() {
    $scope.showSearch = !$scope.showSearch;
    if (!$scope.showSearch) {
      $scope.search = '';
      $scope.refresh();
    }
  }

  $scope.skipMoneyTrnByTempl = function(templ) {
    MoneyTrnTemplsSvc.skip({bsId: $rootScope.bsId}, templ, function() {
      $scope.refresh();
    });
  }

  $scope.cancelTempl = function() {
    $scope.loadTempls();
  };

  $scope.saveTempl = function(templ) {
    $scope.updateTempl(templ);
  };

  $scope.createTemplByTrn = function(trn) {
    MoneyTrnTemplsSvc.create({bsId: $rootScope.bsId}, trn, function() {
      $scope.refresh();
    });
  };

  $scope.updateTempl = function(templ) {
    delete templ.isEdited;
    MoneyTrnTemplsSvc.update({bsId: $rootScope.bsId}, templ, function() {
      $scope.refresh();
    });
  };

  $scope.deleteTempl = function(templ) {
    delete templ.isEdited;
    MoneyTrnTemplsSvc.delete({bsId: $rootScope.bsId}, templ, function() {
      $scope.refresh();
    });
  };
}
