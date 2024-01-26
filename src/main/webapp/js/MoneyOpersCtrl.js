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
  $scope.tags;
  $scope.suggestTags;

  $scope.$on('login', function() {
    $scope.loadRecurrenceOpers();
    $scope.loadOpersFirstPage($scope.pageSize);
    $scope.loadAccounts();
    $scope.loadBalances();
    $scope.loadTags();
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

  $scope.$on('refreshMoneyOpers', function() {
    $scope.refresh();
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
    $scope.loadTags();
  }

  $scope.loadAccounts = function() {
    if (!$scope.isLogged()) return;
    let response = AccountsSvc.query(function() {
      $scope.accounts = response.data.filter(function(account) {
        return !account['isArc'];
      });
    });
  };

  $scope.loadBalances = function() {
    if (!$scope.isLogged()) return;
    let response = BalancesSvc.query(function() {
      $scope.balances = response.data;
    })
  }

  $scope.getGroupList = function(caption) {
    let result;
    $scope.opers.data.forEach(function(groupList) {
      if (typeof result === 'undefined' && groupList.caption === caption) {
        result = groupList;
      }
    });
    if (typeof result === 'undefined') {
      result = {caption: caption, isDone: caption !== 'Новые' && caption !== 'Ближайшие', expanded: true, items: []};
      if (caption === 'Ближайшие') {
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
    let today = $scope.getToday();
    list.forEach(function(oper) {
      let groupName = (oper.status === "Done") ? oper.operDate : "Ближайшие";
      let groupList = $scope.getGroupList(groupName);
      groupList.items = groupList.items.concat(oper);
      if (groupName === 'Ближайшие' && (new Date(oper.operDate) - today) === 0) {
        groupList.expanded = true;
      }
    });
  }

  $scope.loadRecurrenceOpers = function() {
    if (!$scope.isLogged()) return;
    let response = RecurrenceOpersSvc.query({search: $scope.search}, function() {
      $scope.recurrenceOpers = response.data;
    });
  }

  $scope.loadOpersFirstPage = function(limit) {
    if (!$scope.isLogged()) return;
    let qLimit = limit;
    if (typeof limit === 'undefined') {
      qLimit = $scope.pageSize;
    }
    let response = MoneyOpersSvc.query({search: $scope.search, limit: qLimit, offset: 0}, function() {
      $scope.opers = {data: [], hasNext: response.data['paging'].hasNext};
      $scope.addToOpers(response.data.items);
    });
  };

  $scope.loadOpersNextPage = function(search) {
    let offset = $scope.getOpersLength();
    let response = MoneyOpersSvc.query({search: search, limit: $scope.pageSize, offset: offset}, function () {
      $scope.addToOpers(response.data.items);
      $scope.opers.hasNext = response.data['paging'].hasNext;
    });
  };

  // Возвращает количество завершенных операций в $scope.opers.
  $scope.getOpersLength = function() {
    let length = 0;
    $scope.opers.data.forEach(function(groupList) {
      length += groupList.isDone ? groupList.items.length : 0;
    });
    return length;
  }

  $scope.hasOpersNextPage = function() {
    if (typeof $scope.opers === 'undefined' || typeof $scope.opers.data === 'undefined') {
      return false;
    }
    return $scope.opers.hasNext;
  };

  $scope.getDefaultDate = function() {
    if (typeof $scope.defaultDate === 'undefined') {
      $scope.defaultDate = $scope.getToday();
    }
    return $scope.defaultDate;
  };

  $scope.getToday = function() {
    let today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
  }

  $scope.getFirstAccounts = function(oper) {
    if (typeof($scope.accounts) === 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      let items = oper.items;
      if (oper.type === 'transfer' && items.length === 2) {
        if (account.type === 'expense' || account.type === 'income') {
          return false;
        }
        let result = true;
        let toBalanceId = items[1]['balanceId']
        if (toBalanceId !== null) {
          result = account.id !== toBalanceId;
          if (result) {
            let toAccount = $scope.getAccount(toBalanceId);
            if (toAccount.type === 'reserve' || toAccount.type === 'service') {
              result = account.type === 'reserve' || account.type === 'service';
            } else {
              result = account.type !== 'reserve' && account.type !== 'service';
            }
          }
        }
        return result;
      } else {
        return account.type === 'debit' || account.type === 'credit' || account.type === 'asset' || account.type === 'reserve';
      }
    });
  }

  $scope.getSecondAccounts = function(oper) {
    if (typeof $scope.accounts === 'undefined') {
      return [];
    }
    return $scope.accounts.filter(function(account) {
      if (oper.type === 'transfer') {
        if (account.type === 'expense' || account.type === 'income') {
          return false;
        }
        let result = true;
        let items = oper.items;
        let fromBalanceId = items[0]['balanceId']
        if (fromBalanceId !== null) {
          result = account.id !== fromBalanceId;
          if (result) {
            let fromAccount = $scope.getAccount(fromBalanceId);
            if (fromAccount.type === 'reserve' || fromAccount.type === 'service') {
              result = account.type === 'reserve' || account.type === 'service';
            } else {
              result = account.type === 'debit' || account.type === 'credit' || account.type === 'asset';
            }
          }
        }
        return result;
      } else {
        return account.type === oper.type;
      }
    });
  }

  $scope.loadTags = function() {
    let response = MoneyOpersSvc.tags(function() {
      $scope.tags = response.data;
    });
  }

  $scope.getAccount = function(id) {
    return $scope.accounts.filter(function(account) {
      return account.id === id;
    })[0];
  }

  $scope.getBalance = function(id) {
    return $scope.balances.filter(function(balance) {
      return balance.id === id
    })[0]
  }

  $scope.newOper = function(param) {
    let performedAt = formatDate($scope.getDefaultDate());
    let oper;
    if (typeof param.id !== 'undefined') {
      let templateOper = param;
      oper = {type: templateOper.type, comment: templateOper.comment, tags: templateOper.tags,
        period: templateOper.period, items: templateOper.items};
      for (let item of oper.items) {
        item.id = randomUUID()
        item.performedAt = performedAt
      }
    } else {
      let type = param;
      let sgn;
      if (type === 'income') sgn = 1; else sgn = -1;
      let items = [{id: randomUUID(), balanceId: null, value: null, sgn: sgn, performedAt: performedAt, index: 0}]
      oper = {type: type, items: items, tags: []};
      if (type === 'transfer') {
        items.push({id: randomUUID(), balanceId: null, value: null, sgn: 1, performedAt: performedAt, index: 1})
      }
    }
    oper.isNew = true;
    oper.status = 'Done';
    oper.operDate = performedAt;
    oper.isEdited = true;
    let groupList = $scope.getGroupList('Новые');
    groupList.items.splice(0, 0, oper);
  };

  $scope.validOper = function(oper) {
    let isValid;
    if (oper.type === 'transfer') {
      isValid = oper.items.length === 2;
      if (isValid) {
        oper.items[1]['value'] = oper.items[0]['value'];
      }
    } else {
      isValid = oper.items.length === 1;
    }
    isValid &&= oper.items.reduce(function(checkResult, operItem) {
      return checkResult && operItem.balanceId !== null && operItem.value !== null;
    }, true)
    return isValid;
  }

  $scope.completeOper = function(oper) {
    let today = formatDate(new Date());
    if (oper.operDate > today) {
      oper.operDate = today;
    }
    oper.isNew = oper.status === 'Recurrence' || oper.status === 'Trend';
    oper.status = 'Done';
    oper.isEdited = true;
  }

  $scope.cancelOper = function() {
    $scope.refreshOpers();
  };

  $scope.saveOper = function(oper) {
    delete oper.isEdited;
    for (let item of oper.items) {
      let value = item.value;
      if (typeof(value) === 'string' && value.includes(',')) {
        item.value = value.replace(',', '.')
      }
      item.performedAt = oper.operDate;
    }
    if (oper.isNew || oper.status === 'Recurrence') {
      $scope.createOper(oper);
    } else {
      $scope.updateOper(oper);
    }
    $scope.defaultDate = new Date(oper.operDate);
  };

  $scope.createOper = function(oper) {
    oper.id = randomUUID();
    let response = MoneyOpersSvc.create(oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength() + response.data.length);
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.updateOper = function(oper) {
    MoneyOpersSvc.update(oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.deleteOper = function(oper) {
    delete oper.isEdited;
    MoneyOpersSvc.delete(oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  };

  $scope.skipOper = function(oper) {
    MoneyOpersSvc.skip(oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
      $scope.loadRecurrenceOpers();
      $rootScope.$broadcast('refreshBalanceSheet');
    });
  }

  $scope.upOper = function(oper) {
    delete oper.isEdited;
    MoneyOpersSvc.up(oper, function() {
      $scope.loadOpersFirstPage($scope.getOpersLength());
    });
  };

  // item - экземпляр Oper или RecurrenceOper.
  $scope.newTagKeyPressed = function(event, tag, item) {
    if (event.keyCode === 13) {
      addTag(item, tag);
    } else {
      $scope.getSuggestTags(item, tag);
    }
  };

  // item - экземпляр Oper или RecurrenceOper.
  function addTag(item, tag) {
    if (typeof tag !== 'undefined' && tag.length > 0) {
      let tags = item['tags'];
      tags.splice(tags.length, 0, tag);
    }
  }

  // item - экземпляр Oper или RecurrenceOper.
  $scope.editTagKeyPressed = function(event, index, tag, item) {
    if (event.keyCode === 13) {
      let tags = item['tags'];
      if (typeof tag !== 'undefined' && tag.length > 0) {
        tags[index] = tag;
      } else {
        tags.splice(index, 1);
      }
    }
  };

  $scope.getPeriodName = function(oper) {
    let period = oper['period'];
    return (period === 'Day') ? 'День' :
        (period === 'Month') ? 'Месяц' :
        (period === 'Year') ? 'Год' :
        (period === 'Single') ? 'Разовая' : '?';
  };

  $scope.formatMoney = function(amount, currencySymbol) {
    return $rootScope.formatMoney(amount, currencySymbol);
  };

  $scope.showSearchClick = function() {
    $scope.showSearch = !$scope.showSearch;
    if (!$scope.showSearch) {
      $scope.search = '';
      $scope.refresh();
    }
  };

  $scope.skipMoneyOperByRecurrenceOper = function(recurrenceOper) {
    RecurrenceOpersSvc.skip(recurrenceOper, function() {
      $scope.refresh();
    });
  };

  $scope.cancelRecurrenceOper = function() {
    $scope.loadRecurrenceOpers();
  };

  $scope.saveRecurrenceOper = function(recurrenceOper) {
    $scope.updateRecurrenceOper(recurrenceOper);
  };

  $scope.createRecurrenceOperByOper = function(oper) {
    RecurrenceOpersSvc.create(oper, function() {
      $scope.refresh();
    });
  };

  $scope.updateRecurrenceOper = function(recurrenceOper) {
    delete recurrenceOper.isEdited;
    RecurrenceOpersSvc.update(recurrenceOper, function() {
      $scope.refresh();
    });
  };

  $scope.deleteRecurrenceOper = function(recurrenceOper) {
    delete recurrenceOper.isEdited;
    RecurrenceOpersSvc.delete(recurrenceOper, function() {
      $scope.refresh();
    });
  };

  /**
   * Возвращает true, если счета, заданные идентификаторами @param id1 и @param id2, в разных валютах.
   * Если хотя бы один из счетов не определен или счета в одной валюте, возвращает false.
   */
  $scope.balanceCurrenciesIsNotEqual = function(id1, id2) {
    if (typeof id1 === 'undefined' || typeof id2 === 'undefined') {
      return false;
    }
    let balance1 = $scope.getBalance(id1);
    let balance2 = $scope.getBalance(id2);
    if (typeof balance1 === 'undefined' || typeof balance2 === 'undefined') {
      return false;
    }
    return balance1['currencyCode'] !== balance2['currencyCode'];
  }

  $scope.getAmountAsHtml = function(oper) {
    let arrow = '';
    if (oper.type !== 'transfer') {
      if (oper.items[0].sgn > 0) arrow = '↗'; else arrow = '↘';
    }
    let result = arrow + '&nbsp;' + $scope.formatMoney($scope.getOperAmount(oper), oper.items[0]['currencySymbol']);
    if (oper.type === 'transfer' && oper.items.length > 1 && oper.items[0]['currencyCode'] !== oper.items[1]['currencyCode']) {
      result = result + " (" + $scope.formatMoney(oper.items[1]['value'], oper.items[1]['currencySymbol']) + ")";
    }
    return result;
  }

  $scope.getOperAmount = function(oper) {
    return oper.items[0]['value'];
  };

  $scope.getSuggestTags = function(oper, search) {
    let response = MoneyOpersSvc.suggestTags({operType: oper.type, search: search, tags: oper.tags}, function() {
      $scope.suggestTags = response.data;
    });
  }
}
