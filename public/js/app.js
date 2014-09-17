var app = angular.module('coinport.admin', ['ui.bootstrap', 'ngRoute']);

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
            case 8: return 'CANCELLED';
            case 9: return 'REJECTED';
            case 10: return 'HOT_INSUFFICIENT';
            case 11: return 'PROCESSING';
            case 12: return 'BITWAY_FAILED';
            case 13: return 'PROCESSED_FAIL';
            case 14: return 'CONFIRM_BITWAY_FAIL';
            case 15: return 'REORGING_FAIL';
            case 16: return 'HOT_INSUFFICIENT_FAIL';
            case -1: return 'Operating...';
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
            case 0: return 'Chinese';
            case 1: return 'English';
            default: return 'Chinese';
        }
    }
});

app.filter('transferStatusClass', function() {
    return function(input) {
        if (input == 4) return 'success';
        if (input == 5) return 'danger';
        return 'warning';
    };
});

app.filter('transferOperationText', function() {
    return function(input) {
        return Messages.transfer.operation[input];
    };
});

app.filter('transferSign', function() {
    return function(input) {
        if (input == 0) return '+';
        if (input == 1) return '-';
        return '';
    };
});

app.filter('transferOperationClass', function() {
    return function(input) {
        if (input == 0) return 'success';
        if (input == 1) return 'warning';
        return '';
    };
});

app.filter('quantity', function() {
    var filter = function(input) {
        if (!input) return 0;
        if (input > 1e-6) return +input.toFixed(8);
        var s = input.toFixed(8);
        return s;
    }
    return filter;
});

app.filter('price', function() {
    return function(input) {
        if (!input) return 0;
        if (input > 1e-6) return +input.toFixed(8);
        var s = input.toFixed(8);
        return s;
    };
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
        when('/deposit', {
            controller: 'DepositCtrl',
            templateUrl: 'views/deposit.html'
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

    $scope.getRegisteredUserCount = function() {
        $http.get('/user/totalcount')
            .success(function(data, status, headers, config) {
                $scope.totalRegisteredUserCount = data.data;
            })
    };

    $scope.getRegisteredUserCount();
}]);

app.controller('NotifyCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.ntypes = [
    {text: 'Warning', value: 'Warning'},
    {text: 'Danger', value: 'Danger'},
    {text: 'Success', value: 'Success'},
    {text: 'Info', value: 'Info'}];

    $scope.languages = [
        {text: 'Chinese', value: 'Chinese'},
        {text: 'English', value: 'English'}];


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
        {text: 'REORGING_SUCCEEDED', value: 'REORGING_SUCCEEDED'},
        {text: 'CANCELLED', value: 'CANCELLED'},
        {text: 'REJECTED', value: 'REJECTED'},
        {text: 'HOT_INSUFFICIENT', value: 'HOT_INSUFFICIENT'},
        {text: 'PROCESSING', value: 'PROCESSING'},
        {text: 'BITWAY_FAILED', value: 'BITWAY_FAILED'},
        {text: 'PROCESSED_FAIL', value: 'PROCESSED_FAIL'},
        {text: 'CONFIRM_BITWAY_FAIL', value: 'CONFIRM_BITWAY_FAIL'},
        {text: 'REORGING_FAIL', value: 'REORGING_FAIL'},
        {text: 'HOT_INSUFFICIENT_FAIL', value: 'HOT_INSUFFICIENT_FAIL'}
    ];

    $scope.currencyList = COINPORT.currencyList;

    $scope.hotWallets = [];
    $scope.coldWallets = [];

    $scope.query = {page: 1, limit: 20};

    $scope.loadTransfer = function() {
        $scope.query.currency = $scope.currency;
        console.log($scope.query);
        $http.get('/transfer/get', {params: $scope.query})
            .success(function (data, status, headers, config) {
                $scope.transfers = data.data;
            });
    };

    $scope.transferConfirm = function(item) {
        item.status = -1;
        $http.post('/transfer/confirm/' + item.id, {})
            .success(function(data, status, headers, config) {
                console.log('request:', $scope.notification, ' response:', data);
                setTimeout($scope.loadTransfer, 1000);
            });
    };

    $scope.transferReject = function(item) {
        item.status = -1;
        $http.post('/transfer/reject/' + item.id, {})
            .success(function(data, status, headers, config) {
                console.log('request:', $scope.notification, ' response:', data);
                setTimeout($scope.loadTransfer, 1000);
            });
    };

    $scope.loadWallets = function() {
        $scope.addressUrl = COINPORT.addressUrl[$scope.currency];
        $http.get('/api/open/wallet/' + $scope.currency + '/hot')
            .success(function(data, status, headers, config) {
                $scope.hotWallets = data.data.reverse();
                console.log($scope.hotWallets);
            });

        $http.get('/api/open/wallet/' + $scope.currency + '/cold')
            .success(function(data, status, headers, config) {
                $scope.coldWallets = data.data.reverse();
                console.log($scope.coldWallets);
            });
    };

    $scope.showConfirm = function(item) {
        return (item.status == 0 || item.status == 7 ||
                item.status == 10 || item.status == 11 || item.status == 12) &&
            (item.operation == 0 || item.operation == 1 || item.operation == 4)
    };

    $scope.showReject = function(item) {
        return (item.status == 0 || item.status == 7 ||
                item.status == 10 || item.status == 11 || item.status == 12) &&
            (item.operation == 0 || item.operation == 1 || item.operation == 4)
    };

    $scope.reload = function() {
        $scope.loadTransfer();
        $scope.loadWallets();
    };

    $scope.loadTransfer();

}]);

app.controller('DepositCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.currencyList = COINPORT.currencyList;
    $scope.payload = {uid: '1000000000', currency: 'BTSX'};
    $scope.deposit = function() {
        if (!$scope.payload.currency || $scope.payload.currency=='ALL') {
            alert('Select currency to deposit');
            return;
        }
        if (!$scope.payload.uid || isNaN($scope.payload.uid) || (+$scope.payload.uid < 1000000000)) {
            alert('Input UID of user whom you want to deposit to');
            return;
        }
        if (!$scope.payload.amount || isNaN($scope.payload.amount)) {
            alert('Input Amount of coin to deposit');
            return;
        }
        console.log('deposit', $scope.payload);

        $http.post('/transfer/deposit', $.param($scope.payload))
          .success(function(data, status, headers, config) {
            console.log('response', data);
            alert(data.message);
            window.location.hash='#/transfer';
        });
    };
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

// Directives
// nav bar
app.directive('cpNav', function($window) {
 'use strict';
 return {
   restrict: 'A',
   link: function postLink(scope, element, attrs, controller) {
     // Watch for the $window
     scope.$watch(function() {
       return $window.location.hash;
     }, function(newValue, oldValue) {

       $('[route]', element).each(function(k, elem) {
         var $elem = angular.element(elem),
           pattern = $elem.attr('route'),
           regexp = new RegExp('^' + pattern + '$', ['i']);
         if(regexp.test(newValue)) {
           $elem.addClass('active');
         } else {
           $elem.removeClass('active');
         }
       });
     });
   }
 };
})
