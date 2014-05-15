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
    $scope.reload = function() {
        $http.get('/notifications', {params: {}})
          .success(function(data, status, headers, config) {
            $scope.notifications = data.data;
            console.log(data);
        })
    };

    $scope.add = function() {
        var payload = {message: $scope.message};
        $http.post('/notifications/add', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('request:', payload, ' response:', data);
            $scope.reload();
        });
    };

    $scope.remove = function(id) {
        $http.post('/notifications/remove/' + id)
          .success(function(data, status, headers, config) {
            $scope.reload();
        });
    };

    $scope.reload();
}]);

app.controller('DepositCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.snapshots = 12;
}]);