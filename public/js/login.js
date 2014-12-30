/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

(function() {
    var app = angular.module('admin.login', []);

    function httpConfig($httpProvider) {
        $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
        //    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
    }
    app.config(httpConfig);

    app.controller('LoginController', ['$http', '$window', function($http, $window) {
        this.email = '';
        this.password = '';
        this.emailUuid = '';
        this.emailCode = '';
        this.phoneUuid = '';
        this.phoneCode = '';

        var self = this;
        this.login = function() {
            var loginInfo = {
                'email': this.email,
                'password': this.password,
                'emailuuid' : this.emailUuid,
                'emailcode' : this.emailCode,
            };
            $http.post('login', $.param(loginInfo)).success(function(data, status, headers, config) {
                if (data.success === true) {
                    $window.location.href = '/#/';
                } else {
                    alert('login failed');
                }
            }).error(function(data, status, headers, config) {
            });
        };

        this.getEmailCode = function() {
            $http.get('/emailverification', {params: {email: this.verifyEmail}})
                .success(function (data, status, headers, config) {
                    if (data.success) {
                        alert('code sent!');
                        self.emailUuid = data.data;
                    } else {
                        alert(data.message);
                    }
                });
        };

        this.getPhoneCode = function() {
            $http.get('/smsverification2')
                .success(function (data, status, headers, config) {
                    if (data.success) {
                        $scope.verifyCodeUuidMobile = data.data;
                    } else {
                        $scope.showBindMobileError = true;
                        $scope.bindMobileError = Messages.getMessage(data.code, data.message)
                    }
                });
        };
    }]);
})();
