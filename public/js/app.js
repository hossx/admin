var app = angular.module('coinport.admin', ['ngRoute']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        controller: 'DashboardCtrl',
        templateUrl: 'views/dashboard.html'
    }).
    when('/deposit', {
        controller: 'DepositCtrl',
        templateUrl: 'views/deposit.html'
    }).
    otherwise({
        redirectTo: '/'
    });
}

function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

app.config(routeConfig);
app.config(httpConfig);

app.controller('DashboardCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.snapshots = 12;
}]);

app.controller('DepositCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.snapshots = 12;
}]);