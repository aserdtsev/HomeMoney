'use strict';

hmControllers.controller('MoneyOpersCtrl',
    ['$scope', '$rootScope', 'AccountsSvc', 'BalancesSvc', 'MoneyOpersSvc', 'RecurrenceOpersSvc', MoneyOpersCtrl]);
function MoneyOpersCtrl($scope, $rootScope, AccountsSvc, BalancesSvc, MoneyOpersSvc, RecurrenceOpersSvc) {
  $scope.defaultDate;
  $scope.pageSize = 5;
  $scope.accounts;
  $scope.balances;
  $scope.search = '';
  $scope.opers;
  $scope.recurrenceOpers;
  $scope.labels;

  $scope.$on('login', function() {
    $scope.loadRecurrenceOpers();
    $scope.loadOpersFirstPage($scope.pageSize);
    $scope.loadAccounts();
    $scope.loadBalances();
    $scope.loadLabels();
  });

  $scope.$on('logout', function() {
    $scope.recurrenceOpers = undefined;
    $scope.accounts = undefined;
    $scope.fromAccounts = undefined;
    $scope.toAccounts = undefined;
    $scope.opers = undefined;
    $scope.search = undefined;
  });

  $scope.$on('refreshAccounts', function() {
    $scope.loadAccounts();
    $scope.loadBalances();
  });

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.find = function(search) {
    $scope.search = search;
    $scope.refresh();
  }

  $scope.refresh = function() {
    $scope.loadRecurrenceOpers();
    $scope.refreshOpers();
    $rootScope.$broadcast('refreshBalanceSheet');
  }

  $scope.refreshOpers = function() {
    $scope.loadOpersFirstPage($scope.getOpersLength());
    $scope.loadLabels();
  }

  $scope.loadAccounts = function() {
    if (!$scope.isLogged()) return;
    var response = AccountsSvc.query({bsId: $rootScope.bsId}, function() {
      $scope.accounts = response.data.filter(function(account) {
        return !account['isArc'];
      });
    });
  };

  $scope.loadBalances = function() {
    if (!$scope.isLogged()) return;
    var response = BalancesSvc.query({bsId: $rootScope.bsId}, function() {
      $scope.balances = response.data;
    })
  }

  $scope.getGroupList = function(caption) {
    var result;
    $scope.opers.data.forEach(function(groupList) {
      if (typeof result == 'undefined' && groupList.caption == caption) {
        result = groupList;
      }
    });
    if (typeof result == 'undefined') {
      result = {caption: caption, isDone: caption != 'Новые' && caption != 'Ближайшие', expanded: true, items: []};
      if (caption == 'Ближайшие') {
        result.expanded = false;
      }
      $scope.opers.data.splice(0, 0, result);
      $scope.opers.data.sort(function(a, b) {
        if (a.caption < b.caption) return 1;
        if (a.caption > b.caption) return -1;
      });
    }
    return result;
  }

  $scope.addToOpers = function(list) {
    var today = $scope.getToday();
    list.forEach(function(oper) {
      var groupName = (oper.status == "done") ? oper.operDate : "Ближайшие";
      var groupList = $scope.getGroupList(groupName);
      groupList.items = groupList.items.concat(oper);
      if (groupName == 'Ближайшие' && (new Date(oper.operDate) - today) == 0) {
        groupList.expanded = true;
      }
    });
  }

  $scope.loadRecurrenceOpers = function() {
    if (!$scope.isLogged()) return;
    var response = RecurrenceOpersSvc.query({bsId: $rootScope.bsId, search: $scope.search}, function() {
      $scope.recurrenceOpers = response.data;
    });
  }

  $scope.loadOpersFirstPage = function(limit) {
    if (!$scope.isLogged()) return;
    var qLimit = limit;
    if (typeof limit == 'undefined') {
      qLimit = $scope.pageSize;
    }
    var response = MoneyOpersSvc.query({bsId: $rootScope.bsId, search: $scope.search, limit: qLimit, offset: 0}, function() {
      $scope.opers = {data: [], hasNext: response.data.paging.hasNext};
      $scope.addToOpers(response.data.items);
    });
  };

  $scope.loadOpersNextPage = function(search) {
    var offset = $scope.getOpersLength();
    var response = MoneyOpersSvc.query({bsId: $rootScope.bsId, search: search, limit: $scope.pageSize, offset: offset}, function () {
      $scope.addToOpers(response.data.items);
      $scope.opers.hasNext = response.data.paging.hasNext;
    });
  };

  // Возвращает количество завершенных операций в $scope.opers.
  $scope.getOpersLength = function() {
    var length = 0;
    $scope.opers.data.forEach(function(groupList) {
      length += groupList.isDone ? groupList.items.length : 0;
    });
    return length;
  }

  $scope.hasOpersNextPage = function() {
    if (typeof $scope.opers == 'undefined' || typeof $scope.opers.data == 'undefined') {
      return false;
    }
    return $scope.opers.hasNext;
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

  $scope.getFirstAccounts = function(oper) {
    if (typeof $scope.accounts == 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      if (oper.type == 'transfer') {
        if (account.type == 'expense' || account.type == 'income') {
          return false;
        }
        var result = true;
        if (typeof oper.toAccId != 'undefined') {
          result = account.id != oper.toAccId;
          if (result) {
            var toAccount = $scope.getAccount(oper.toAccId);
            if (toAccount.type == 'reserve' || toAccount.type == 'service') {
              result = account.type == 'reserve' || account.type == 'service';
            } else {
              result = account.type != 'reserve' && account.type != 'service';
            }
          }
        }
        return result;
      } else {
        return account.type == 'debit' || account.type == 'credit' || account.type == 'asset';
      }
    });
  }

  $scope.getSecondAccounts = function(oper) {
    if (typeof $scope.accounts == 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      if (oper.type == 'transfer') {
        var result = true;
        if (typeof oper.fromAccId != 'undefined') {
          result = account.id != oper.fromAccId;
          if (result) {
            var fromAccount = $scope.getAccount(oper.fromAccId);
            if (fromAccount.type == 'reserve' || fromAccount.type == 'service') {
              result = account.type == 'reserve' || account.type == 'service';
            } else {
              result = account.type == 'debit' || account.type == 'credit';
            }
          }
        }
        return result;
      } else {
        return account.type == oper.type;
      }
    });
  }

  $scope.loadLabels = function() {
    var response = MoneyOpersSvc.labels({bsId: $rootScope.bsId}, function() {
      $scope.labels = response.data;
    });
  }

  $scope.getAccount = function(id) {
    return $scope.accounts.filter(function(account) {
      return account.id == id;
    })[0];
  }

  $scope.getBalance = function(id) {
    return $scope.balances.filter(function(balance) {
      return balance.id == id
    })[0]
  }

  $scope.newOper = function(param) {
    var oper;
    if (typeof param.id != 'undefined') {
      var recurrenceOper = param;
      oper = {type: recurrenceOper.type, fromAccId: recurrenceOper.fromAccId, amount: recurrenceOper.amount, toAccId: recurrenceOper.toAccId,
        comment: recurrenceOper.comment, labels: recurrenceOper.labels, period: recurrenceOper.period, recurrenceOperId: recurrenceOper.id};
    } else {
      var type = param;
      oper = {type: type, labels: []};
    }
    oper.status = 'doneNew';
    oper.operDate = formatDate($scope.getDefaultDate());
    oper.isEdited = true;
    var groupList = $scope.getGroupList('Новые');
    groupList.items.splice(0, 0, oper);
  };

  $scope.completeOper = function(oper) {
    var today = formatDate(new Date());
    if (oper.operDate > today) {
      oper.operDate = today;
    }
    oper.status = oper.status == 'recurrence' ? 'doneNew' : 'done';
    oper.isEdited = true;
  }

  $scope.cancelOper = function() {
    $scope.refreshOpers();
  };

  $scope.saveOper = function(oper) {
    delete oper.isEdited;
    if (oper.currencyCode == oper.toCurrencyCode) {
      oper.toAmount = oper.amount
    }
    if (oper.status == 'doneNew') {
      $scope.createOper(oper);
    } else {
      $scope.updateOper(oper);
    }
    $scope.defaultDate = new Date(oper.operDate);
  };

  $scope.createOper = function(oper) {
    oper.id = randomUUID();
    var response = MoneyOpersSvc.create({bsId: $rootScope.bsId}, oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength() + response.data.length);
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.updateOper = function(oper) {
    MoneyOpersSvc.update({bsId: $rootScope.bsId}, oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.deleteOper = function(oper) {
    delete oper.isEdited;
    MoneyOpersSvc.delete({bsId: $rootScope.bsId}, oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.skipOper = function(oper) {
    MoneyOpersSvc.skip({bsId: $rootScope.bsId}, oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $scope.loadRecurrenceOpers();
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  }

  $scope.upOper = function(oper) {
    delete oper.isEdited;
    MoneyOpersSvc.up({bsId: $rootScope.bsId}, oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
    });
  };

  // item - экземпляр Oper или RecurrenceOper.
  $scope.newLabelKeyPressed = function(event, label, item) {
    if (event.keyCode == 13) {
      addLabel(item, label);
    }
  };

  // item - экземпляр Oper или RecurrenceOper.
  function addLabel(item, label) {
    if (typeof label != 'undefined' && label.length > 0) {
      var labels = item['labels'];
      labels.splice(labels.length, 0, label);
    }
  }

  // item - экземпляр Oper или RecurrenceOper.
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

  $scope.getSignedAmount = function(oper) {
    return oper['amount'] * (oper['type'] == 'e' ? -1 : 1);
  };

  $scope.getPeriodName = function(oper) {
    var period = oper['period'];
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

  $scope.skipMoneyOperByRecurrenceOper = function(recurrenceOper) {
    RecurrenceOpersSvc.skip({bsId: $rootScope.bsId}, recurrenceOper, function() {
      $scope.refresh();
    });
  }

  $scope.cancelRecurrenceOper = function() {
    $scope.loadRecurrenceOpers();
  };

  $scope.saveRecurrenceOper = function(recurrenceOper) {
    $scope.updateRecurrenceOper(recurrenceOper);
  };

  $scope.createRecurrenceOperByOper = function(oper) {
    RecurrenceOpersSvc.create({bsId: $rootScope.bsId}, oper, function() {
      $scope.refresh();
    });
  };

  $scope.updateRecurrenceOper = function(recurrenceOper) {
    delete recurrenceOper.isEdited;
    RecurrenceOpersSvc.update({bsId: $rootScope.bsId}, recurrenceOper, function() {
      $scope.refresh();
    });
  };

  $scope.deleteRecurrenceOper = function(recurrenceOper) {
    delete recurrenceOper.isEdited;
    RecurrenceOpersSvc.delete({bsId: $rootScope.bsId}, recurrenceOper, function() {
      $scope.refresh();
    });
  };

  /**
   * Возвращает true, если счета, заданные идентификаторами @param id1 и @param id2, в разных валютах.
   * Если хотя бы один из счетов не определен или счета в одной валюте, возвращает false.
   */
  $scope.balanceCysIsNotEqual = function(id1, id2) {
    if (typeof id1 == 'undefined' || typeof id2 == 'undefined') {
      return false;
    }
    var balance1 = $scope.getBalance(id1);
    var balance2 = $scope.getBalance(id2);
    if (typeof balance1 == 'undefined' || typeof balance2 == 'undefined') {
      return false;
    }
    return balance1['currencyCode'] != balance2['currencyCode'];
  }

  $scope.getAmountAsHtml = function(oper) {
    var result = $scope.formatMoney($scope.getSignedAmount(oper), oper['currencySymbol'])
    if (oper['currencyCode'] != oper['toCurrencyCode'])
      result = result + " (" + $scope.formatMoney(oper['toAmount'], oper['toCurrencySymbol']) + ")"
    return result
  }
}
