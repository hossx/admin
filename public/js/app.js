var app = angular.module('coinport.admin', ['ngRoute']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        controller: 'DashboardCtrl',
        templateUrl: 'views/dashboard.html'
    }).
    when('/notification', {
        controller: 'NotifyCtrl',
        templateUrl: 'views/notification.html'
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

app.controller('NotifyCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.ntypes = [
    {text: 'Warning', value: 'Warning'},
    {text: 'Danger', value: 'Danger'},
    {text: 'Success', value: 'Success'},
    {text: 'Info', value: 'Info'}];

    $scope.notification = {}
    $scope.notification.ntype = "Info"

    $scope.reload = function() {
        $http.get('/notifications/get', {params: {}})
          .success(function(data, status, headers, config) {
            $scope.notifications = data.data.items;
        })
    };

    $scope.save = function() {
        $http.post('/notifications/set', $.param($scope.notification))
          .success(function(data, status, headers, config) {
            console.log('request:', $scope.notification, ' response:', data);
            $scope.reload();
        });
    };

    $scope.remove = function(item) {
        item.removed = true
        $http.post('/notifications/set', $.param(item))
          .success(function(data, status, headers, config) {
            $scope.reload();
        });
    };

    $scope.reload();
}]);

app.controller('DepositCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.snapshots = 12;
}]);