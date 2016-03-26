'use strict';

/* Services */

var hmServices = angular.module('hmServices', ['ngResource']);

hmServices.factory('UserSvc', ['$resource',
  function($resource) {
    return $resource('api/user', {}, {
      getBalanceSheetId: { method: 'GET', url: 'api/user/balance-sheet-id' },
      login: { method: 'POST', params: { email: '@email', pwd: '@pwd' }, url: 'api/user/login'},
      logout: { method: 'POST', url: 'api/user/logout' }
    })
  }
]);

hmServices.factory('BalanceSheetSvc', ['$resource',
  function($resource) {
    return $resource('api/:bsId/bs-stat', {}, {
      query: { method: 'GET' }
    })
  }
]);

hmServices.factory('ReferencesSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/references';
    return $resource(baseUrl, {}, {
      currencies: { method: 'GET', url: baseUrl + '/currencies' }
    })
  }
]);

hmServices.factory('AccountsSvc', ['$resource',
  function($resource) {
    return $resource('api/:bsId/accounts', {}, {
      query: { method: 'GET' }
    })
  }
]);

hmServices.factory('CategoriesSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/categories';
    return $resource(baseUrl, {}, {
      query: { method: 'GET' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' }
    })
  }
]);

hmServices.factory('BalancesSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/balances';
    return $resource(baseUrl, {}, {
      query:  { method: 'GET' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' },
      up:     { method: 'POST', url: baseUrl + '/up' }
    })
  }
]);

hmServices.factory('ReservesSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/reserves';
    return $resource(baseUrl, {}, {
      query: { method: 'GET' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' }
    })
  }
]);

hmServices.factory('MoneyTrnsSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/money-trns';
    return $resource(baseUrl, {}, {
      query: { method: 'GET', params: { search: '@search', offset: '@offset', limit: '@limit' } },
      item: { method: 'GET', url: baseUrl + '/item' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' },
      skip: {method: 'POST', url: baseUrl + '/skip' },
      up: { method: 'POST', url: baseUrl + '/up' }
    })
  }
]);

hmServices.factory('MoneyTrnTemplsSvc', ['$resource',
  function($resource) {
    var baseUrl = 'api/:bsId/money-trn-templs';
    return $resource(baseUrl, {}, {
      query: { method: 'GET', params: { search: '@search' } },
      skip: { method: 'POST', url: baseUrl + '/skip' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' }
    })
  }
]);
