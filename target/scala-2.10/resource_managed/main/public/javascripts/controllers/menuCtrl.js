app.controller('menuCtrl', [
  '$scope', function($scope) {
    return $scope.location = window.location.pathname;
  }
]);