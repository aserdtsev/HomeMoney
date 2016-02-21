'use strict';

hmControllers.controller('CategoriesCtrl',
    ['$scope', '$rootScope', 'CategoriesSvc', CategoriesCtrl]);
function CategoriesCtrl($scope, $rootScope, CategoriesSvc) {
  $scope.rootCategories;
  $scope.categories;

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.getCategories = function(includeArc) {
    if (typeof $scope.categories == 'undefined') {
      return [];
    }
    if (includeArc) {
      return $scope.categories;
    } else {
      return $scope.categories.filter(function(category) {
        return !category['isArc'] || category.isEdited;
      });
    }
  }

  $scope.loadCategories = function() {
    if (!$scope.isLogged()) {
      return;
    }
    var response = CategoriesSvc.query({bsId: $rootScope.bsId}, function() {
      $scope.rootCategories = response.data.filter(function(cat) {
        return cat['rootId'] == null;
      });
      $scope.categories = response.data;
    });
  };

  $scope.refresh = function() {
    $scope.loadCategories();
  }

  $scope.$on('login', function() {
    $scope.loadCategories();
  });

  $scope.$on('logout', function() {
    $scope.rootCategories = undefined;
    $scope.categories = undefined;
  });

  $scope.newCategory = function() {
    $scope.categories.splice(0, 0, {id: null, isArc: false, isEdited: true});
  };

  $scope.saveCategory = function(category) {
    delete category.isEdited;
    if (category.id == null) {
      $scope.createCategory(category);
    } else {
      $scope.updateCategory(category);
    }
  };

  $scope.cancelEditCategory = function() {
    $scope.refresh();
  };

  $scope.createCategory = function(category) {
    category.id = randomUUID();
    CategoriesSvc.create({bsId: $scope.bsId}, category, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.updateCategory = function(category) {
    CategoriesSvc.update({bsId: $scope.bsId}, category, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.deleteCategory = function(category) {
    CategoriesSvc.delete({bsId: $scope.bsId}, category, function() {
      $scope.refresh();
      $rootScope.$broadcast('refreshAccounts');
    });
  };

  $scope.getClass = function(category) {
    var result = category.type;
    if (category.rootId != null) {
      result += ' category-level1';
    }
    return result;
  }
}
