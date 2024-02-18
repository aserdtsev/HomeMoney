'use strict';

/* Services */

const hmServices = angular.module('hmServices', ['ngResource']);

hmServices.factory('UserSvc', ['$resource',
  function($resource) {
    return $resource('api/user', {}, {
      login: { method: 'POST', url: 'api/user/login', hasBody: false }
    })
  }
]);

hmServices.factory('BalanceSheetSvc', ['$resource',
  function($resource) {
    return $resource('api/bs-stat', {}, {
      query: { method: 'GET' }
    })
  }
]);

hmServices.factory('ReferencesSvc', ['$resource',
  function($resource) {
    const baseUrl = 'api/references';
    return $resource(baseUrl, {}, {
      currencies: { method: 'GET', url: baseUrl + '/currencies' }
    })
  }
]);

hmServices.factory('AccountsSvc', ['$resource',
  function($resource) {
    return $resource('api/accounts', {}, {
      query: { method: 'GET' }
    })
  }
]);

hmServices.factory('TagSvc', ['$resource',
  function($resource) {
    const baseUrl = 'api/tags';
    return $resource(baseUrl, {}, {
      query: { method: 'GET' },
      create: { method: 'POST' },
      delete: { method: 'DELETE', url: baseUrl + '/:tagId' },
      update: { method: 'PUT' }
    })
  }
]);

hmServices.factory('BalancesSvc', ['$resource',
  function($resource) {
    const baseUrl = 'api/balances';
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
    const baseUrl = 'api/reserves';
    return $resource(baseUrl, {}, {
      query: { method: 'GET' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' }
    })
  }
]);

hmServices.factory('MoneyOpersSvc', ['$resource',
  function($resource) {
    const baseUrl = 'api/money-opers';
    return $resource(baseUrl, {}, {
      done: { method: 'GET', url: baseUrl + '/done', params: { search: '@search', offset: '@offset', limit: '@limit' } },
      upcoming: { method: 'GET', url: baseUrl + '/upcoming',
        params: { search: '@search', offset: '@offset', limit: '@limit' } },
      item: { method: 'GET', url: baseUrl + '/item' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' },
      skip: {method: 'POST', url: baseUrl + '/skip' },
      up: { method: 'POST', url: baseUrl + '/up' },
      suggestTags: {method: 'GET', url: baseUrl + '/suggest-tags', params: { operType: '@operType', search: '@search', tags: '@tags' } },
      tags: {method: 'GET', url: baseUrl + '/tags'}
    })
  }
]);

hmServices.factory('RecurrenceOpersSvc', ['$resource',
  function($resource) {
    const baseUrl = 'api/recurrence-opers';
    return $resource(baseUrl, {}, {
      query: { method: 'GET', params: { search: '@search' } },
      skip: { method: 'POST', url: baseUrl + '/skip' },
      create: { method: 'POST', url: baseUrl + '/create' },
      delete: { method: 'POST', url: baseUrl + '/delete' },
      update: { method: 'POST', url: baseUrl + '/update' }
    })
  }
]);
