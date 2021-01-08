'use strict';

hmControllers.controller('TagCtrl',
    ['$scope', '$rootScope', 'TagSvc', TagCtrl]);
function TagCtrl($scope, $rootScope, TagSvc) {
  $scope.tags;
  $scope.search;

  $scope.isLogged = function() {
    return $rootScope.isLogged;
  }

  $scope.getTags = function(search, onlyCategories, categoryType, includeArc) {
    if (typeof $scope.tags == 'undefined') {
      return [];
    }
    let tags = $scope.tags;
    let aIncludeArc = typeof(includeArc) != 'undefined' && includeArc
    if (onlyCategories) {
      if (typeof categoryType == 'undefined') categoryType = 'expense'
      tags = tags.filter(function(tag) {
        return tag['isCategory'] && tag['categoryType'] === categoryType || tag.isEdited;
      });
    }
    if (!aIncludeArc) {
      tags = tags.filter(function(tag) {
        return !tag['isArc'] || tag.isEdited;
      });
    }
    if (typeof(search) != 'undefined' && search.length > 1) {
      const normSearch = search.toLowerCase();
      tags = tags.filter(function(tag) {
        return tag['name'].toLowerCase().includes(normSearch);
      });
    }
    return tags;
  }

  $scope.loadTags = function() {
    if (!$scope.isLogged()) {
      return;
    }
    const response = TagSvc.query({bsId: $rootScope.bsId}, function () {
      $scope.tags = response.data;
    });
  };

  $scope.refresh = function() {
    $scope.loadTags();
  }

  $scope.$on('login', function() {
    $scope.loadTags();
  });

  $scope.$on('logout', function() {
    $scope.tags = undefined;
  });

  $scope.getRootCategoryTags = function(forTag) {
    return $scope.tags.filter(function(tag) {
      return tag['isCategory'] === true && tag['rootId'] == null
          && tag['categoryType'] === forTag['categoryType']
          && tag['id'] !== forTag['id'];
    })
  }

  $scope.newTag = function() {
    $scope.tags.splice(0, 0, {id: null, isCategory: false, categoryType: null, rootId: null, isArc: false, isEdited: true});
  };

  $scope.saveTag = function(tag) {
    delete tag.isEdited;
    if (!tag['isCategory']) {
      tag['categoryType'] = null;
      tag['rootId'] = null;
    }
    if (tag.id == null) {
      $scope.createTag(tag);
    } else {
      $scope.updateTag(tag);
    }
  };

  $scope.cancelEditTag = function() {
    $scope.refresh();
  };

  $scope.createTag = function(tag) {
    tag.id = randomUUID();
    TagSvc.create({bsId: $scope.bsId}, tag, function() {
      $scope.refresh();
    });
  };

  $scope.updateTag = function(tag) {
    TagSvc.update({bsId: $scope.bsId}, tag, function() {
      $scope.refresh();
    });
  };

  $scope.deleteTag = function(tag) {
    TagSvc.delete({bsId: $scope.bsId}, tag, function() {
      $scope.refresh();
    });
  };

  $scope.getClass = function(tag) {
    let result = 'tag';
    if (tag['isCategory']) {
      result += '-' + tag['categoryType'];
    }
    return result;
  }
}
