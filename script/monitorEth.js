/**
 *Author: ** - **@**
 *Last modified: 2015-05-18 21:54
 *Filename: monitorEth.js
 *Copyright 2014 ** Inc. All Rights Reserved.
 */

var EthProxy = require('./ethereum').CryptoProxy;

var config = {
    checkInterval: 1000,
    hotAccount: '0x70677ff0cee81bf7ac3ccba187d1b1e3381b198a',
    rpcUrl: '127.0.0.1:8545'
};

var manager = new EthProxy(config);
manager.start();
