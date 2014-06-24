var app = angular.module('coinport.admin', ['ngRoute']);

// Filters
app.filter('tStatus', function() {
    return function(input) {
        switch (input) {
            case 0: return 'PENDING';
            case 1: return 'ACCEPTED';
            case 2: return 'CONFIRMING';
            case 3: return 'CONFIRMED';
            case 4: return 'SUCCEEDED';
            case 5: return 'FAILED';
            case 6: return 'REORGING';
            case 7: return 'REORGING_SUCCEEDED';
            default: return 'UNKNOWN';
        }
    }
});

app.filter('tType', function() {
    return function(input) {
        switch (input) {
            case 0: return 'DEPOSIT';
            case 1: return 'WITHDRAWAL';
            case 2: return 'USER_TO_HOT';
            case 3: return 'HOT_TO_COLD';
            case 4: return 'COLD_TO_HOT';
            case 5: return 'UNKNOWN';
            default: return 'UNKNOWN';
        }
    }
});

app.filter('lang', function() {
    return function(input) {
        switch (input) {
            case 0: return 'CHINESE';
            case 1: return 'ENGLISH';
            default: return 'CHINESE';
        }
    }
});


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
        when('/transfer', {
            controller: 'TransferCtrl',
            templateUrl: 'views/transfer.html'
        }).
        when('/monitor', {
            controller: 'MonitorCtrl',
            templateUrl: 'views/monitor.html'
        }).
        when('/users', {
            controller: 'UserCtrl',
            templateUrl: 'views/users.html'
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

    $scope.notification = {};
    $scope.notification.ntype = "Info";

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
        item.removed = true;
        $http.post('/notifications/set', $.param(item))
          .success(function(data, status, headers, config) {
            $scope.reload();
        });
    };

    $scope.reload();
}]);

app.controller('TransferCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.transferTypes = [
        {text: 'DEPOSIT', value: 'DEPOSIT'},
        {text: 'WITHDRAWAL', value: 'WITHDRAWAL'},
        {text: 'USER_TO_HOT', value: 'USER_TO_HOT'},
        {text: 'HOT_TO_COLD', value: 'HOT_TO_COLD'},
        {text: 'COLD_TO_HOT', value: 'COLD_TO_HOT'},
        {text: 'UNKNOWN', value: 'UNKNOWN'}];

    $scope.transferStatus = [
        {text: 'PENDING', value: 'PENDING'},
        {text: 'ACCEPTED', value: 'ACCEPTED'},
        {text: 'CONFIRMING', value: 'CONFIRMING'},
        {text: 'CONFIRMED', value: 'CONFIRMED'},
        {text: 'SUCCEEDED', value: 'SUCCEEDED'},
        {text: 'FAILED', value: 'FAILED'},
        {text: 'REORGING', value: 'REORGING'},
        {text: 'REORGING_SUCCEEDED', value: 'REORGING_SUCCEEDED'}];

    $scope.currencyList = [
        {text: 'CNY', value: 'CNY'},
        {text: 'BTC', value: 'BTC'},
        {text: 'LTC', value: 'LTC'},
        {text: 'PTS', value: 'PTS'},
        {text: 'DOGE', value: 'DOGE'}];

    $scope.query = {};
    $scope.loadTransfer = function () {
        $scope.query.skip = 0;
        $scope.query.limit = 15;
        $http.get('/transfer/get', {params: $scope.query})
            .success(function (data, status, headers, config) {
                $scope.transfers = data.data.items;
                $scope.count = data.data.count;
            });
    };

    $scope.transferConfirm = function (item) {
        $http.post('/transfer/confirm/' + item.id, {})
            .success(function(data, status, headers, config) {
                console.log('request:', $scope.notification, ' response:', data);
                $scope.loadTransfer();
            });
    };

    $scope.transferReject = function (item) {
        $http.post('/transfer/reject/' + item.id, {})
            .success(function(data, status, headers, config) {
                console.log('request:', $scope.notification, ' response:', data);
                $scope.loadTransfer();
            });
    };
    //todo(xichen) pending to active button, pagination

    $scope.loadTransfer();
}]);

app.controller('MonitorCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.reload = function () {
      $http.get('/monitor/actors/get', {})
          .success(function(data, status, headers, config) {
              console.log("actors", data.data.pathList);
            $scope.pathList = data.data.pathList;
        });
    };

    $scope.reload();
}]);

app.controller('UserCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.search = {};
    $scope.searchResult = [];
    $scope.showError = false;

    $scope.showSearchDiv = true;
    $scope.showSearchResDiv = true;
    $scope.showProfileDiv = false;
    $scope.listUser = function () {
        console.log("search: ", $scope.search);
        $http.get('/user/search', { params: $scope.search })
            .success(function(data, status, headers, config) {
                console.log("result: ", data);
                if (data.success) {
                    $scope.showError = false;
                    $scope.searchResult = data.data;
                } else {
                    $scope.showError = true;
                    $scope.errorMessage = data.message;
                    $scope.searchResult = [];
                }
            });
    };

    $scope.userProfile = {};
    $scope.showUserProfileError = false;
    $scope.getUserProfile = function(uid) {
        for (var i = 0; i < $scope.searchResult.length; i ++) {
            if ($scope.searchResult[i].id == uid) {
                $scope.userProfile = $scope.searchResult[i];
                break;
            }
        }

        $scope.showSearchDiv = false;
        $scope.showSearchResDiv = false;
        $scope.showProfileDiv = true;
    };

    $scope.backToSearch = function () {
        $scope.updateUsers();
        $scope.showSearchDiv = true;
        $scope.showSearchResDiv = true;
        $scope.showProfileDiv = false;
    };

    $scope.updateUsers = function(user) {
        for (var i = 0; i < $scope.searchResult.length; i++) {
            if ($scope.searchResult[i].id == $scope.userProfile.id) {
                $scope.searchResult[i].status = $scope.userProfile.status;
                break;
            }
        }
    };

    $scope.suspendUser = function (uid) {
        console.debug("suspend user: ", uid);
        $http.get('/user/suspend/' + uid)
            .success(function(data, status, headers, config) {
                console.log("result: ", data);
                if (data.success) {
                    $scope.showUserProfileError = false;
                    $scope.userProfile = data.data;
                } else {
                    $scope.showUserProfileError = true;
                    $scope.userProfileError = data.message;
                }
            });
    };

    $scope.resumeUser = function (uid) {
        console.debug("resume user: ", uid);
        $http.get('/user/resume/' + uid)
            .success(function(data, status, headers, config) {
                console.log("result: ", data);
                if (data.success) {
                    $scope.showUserProfileError = false;
                    $scope.userProfile = data.data;
                } else {
                    $scope.showUserProfileError = true;
                    $scope.userProfileError = data.message;
                }
            });
    };

}]);
