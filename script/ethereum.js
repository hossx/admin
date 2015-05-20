/**
 *Author: ** - **@**
 *Last modified: 2015-05-04 15:51
 *Filename: ethereum.js
 *Copyright 2014 ** Inc. All Rights Reserved.
 */

'use strict'

var Async = require('async');
var request = require('request');
var web3 = require('web3');
var mongodb = require('mongodb');

var  server  = new mongodb.Server('localhost', 27017, {auto_reconnect:true});
var  db = new mongodb.Db('eth', server, {safe:true});

var CryptoProxy = module.exports.CryptoProxy = function(opt_config) {
    if (opt_config) {
        opt_config.checkInterval != undefined && (this.checkInterval = opt_config.checkInterval);
        opt_config.rpcUrl != undefined && (this.rpcUrl = opt_config.rpcUrl);
    }

    this.lastIndex = 'last_index';
    this.hotAccount = opt_config.hotAccount;
};

var basicInfo = {
    method: 'POST',
    url: "http://localhost:8545",
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    }
};

CryptoProxy.prototype.rpcRequest_ = function (requestBody, callback) {
    var self = this;
    request({
        method: basicInfo.method,
        url: basicInfo.url,
        timeout: basicInfo.timeout,
        headers: basicInfo.headers,
        body: JSON.stringify(requestBody)
    }, function (error, response, body) {
        if (!error && response.statusCode == 200 && body) {
            var responseBody = JSON.parse(body);
            callback(null, responseBody);
        } else {
            console.error("request_ error", error);
            callback("error", null);
        }
    });
};

CryptoProxy.prototype.convertAmount_ = function(value) {
    return value/100000;
};

CryptoProxy.prototype.start = function() {
    var self = this;
    db.open(function(error, db) {
        if (error) {
            self.start();
        } else {
            self.checkBlockAfterDelay_();
        }
    });
};

CryptoProxy.prototype.checkBlockAfterDelay_ = function(opt_interval) {
    var self = this;
    var interval = self.checkInterval;
    opt_interval != undefined && (interval = opt_interval)
    setTimeout(self.checkBlock_.bind(self), interval);
};

CryptoProxy.prototype.insertTx_ = function(tx) {
    db.collection("txs", {safe:true}, function(error, collection) {
        if (!error) {
            collection.insert(tx, {safe:true}, function(err, result) {
                if (err) {
                    console.log("insert error: ", err);
                } else {
                }
            }); 
        } else {
            console.log("inserTx_ error: ", error);
        }
    });
}

CryptoProxy.prototype.saveLastIndex_ = function(lastIndex, callback) {
     db.collection("cursorC", {safe:true}, function(error, collection) {
         if (!error) {
             collection.save(lastIndex, {safe:true}, function(err, result) {
                 if (err) {
                     console.log(err);
                     callback(err);
                 } else {
                     callback(null, result);
                 }
             }); 
         } else {
             console.log("inserDb error: ", error);
             callback(error);
         }
     });
}

CryptoProxy.prototype.getLastIndex_ = function(callback) {
    db.collection("cursorC", {safe:true}, function(error, collection) {
        if (!error) {
            collection.findOne(function(err, result) {
                if (err) {
                    console.log(err);
                    callback(null, -1);
                } else {
                    console.log(result);
                    var numIndex = -1;
                    if (result) {
                        numIndex = Number(result.index);
                    }
                    callback(null, numIndex);
                }
            }); 
        } else {
            console.log("getLastIndex_ error: ", error);
            callback(null, -1);
        }
    });
}

CryptoProxy.prototype.checkBlock_ = function() {
    var self = this;
    self.getNextCCBlock_(function(error, block){
        if (!error) {
            //console.log("checkBlock_ %j", block);
            var blockHeight = block.index.height;
            for (var i = 0; i < block.txs.length; i++) {
                var ty = null;
                var cps = null;
                if (block.txs[i].outputAddr == self.hotAccount) {
                    ty = "DEPOSIT";
                } else if (block.txs[i].inputAddr == self.hotAccount) {
                    ty = "WITHDRAWAL";
                } else {
                    ty = "UNKNOW";
                }
                if (block.txs[i].amount > 0.001) {
                    var cpid = /^100\d{7}$/;
                    if (block.txs[i].memo.match(cpid)) {
                        cps = "PENDING";
                    } else {
                        cps = "BAD_FORM";
                    }
                } else {
                    cps = "UNDER_LIMIT";
                }
                var time = new Date().getTime();
                var tx = {_id: block.txs[i].txid, blockNum: blockHeight, inputAddr: block.txs[i].inputAddr, 
                          outputAddr: block.txs[i].outputAddr, a: block.txs[i].amount,
                          c: block.txs[i].memo, ty: ty, cps: cps, t: time};
                self.insertTx_(tx);
            }
            self.checkBlockAfterDelay_(0);
        } else {
            self.checkBlockAfterDelay_();
        }
   });
};

CryptoProxy.prototype.getNextCCBlock_ = function(callback) {
    var self = this;
    Async.compose(self.getNextCCBlockSinceLastIndex_.bind(self), 
        self.getLastIndex_.bind(self))(callback);
};

CryptoProxy.prototype.getNextCCBlockSinceLastIndex_ = function(index, callback) {
    var self = this;
    //console.log("getNextCCBlockSinceLastIndex_ index: ", index);
    self.getBlockCount_(function(error, count) {
        //console.log("getNextCCBlockSinceLastIndex_ count: ", count);
        if (error) {
            console.error(error);
            callback(error);
        } else if (index == count) {
            console.log('no new block found');
            callback('no new block found');
        } else {
            var nextIndex = (index == -1) ? count : index + 1;
            console.log("getNextCCBlockSinceLastIndex_ nextIndex: ", nextIndex);
            self.getCCBlockByIndex_(nextIndex, callback);
        }
    });
};

CryptoProxy.prototype.getBlockCount_ = function(callback) {
    var self = this;
    var requestBody = {jsonrpc: '2.0', id: 1, method: "eth_blockNumber", params: []};
    self.rpcRequest_(requestBody, function(error, result) {
        if (error) {
            console.error("getBlockCount_ error: ", error);
            CryptoProxy.invokeCallback_(error, function() {return error}, callback);
        } else {
            var height = web3.toDecimal(result.result);
            console.log("getBlockCount_ height: ", height);
            CryptoProxy.invokeCallback_(error, function() {return height}, callback);
        }
    });
};

CryptoProxy.prototype.getCCBlockByIndex_ = function(index, callback) {
    var self = this;
    //console.log("Enter into getCCBlockByIndex_ index: ", index);
    Async.compose(self.completeTransactions_.bind(self),
        self.getBlockByNumber_.bind(self))(index, function(error, block) {
        if (!error) {
            var lastIndex = {_id: self.lastIndex, index: index};
            self.saveLastIndex_(lastIndex, function(error, result) {
                if (!error) {
                    callback(null, block);
                } else {
                    console.error("getCCBlockByIndex_error: ", error);
                    callback(error);
                }
            });
        } else {
            console.error("getCCBlockByIndex_ error: ", error);
            callback(error, null);
        }
    });
};

CryptoProxy.prototype.getCCTxByTxHash_ = function(tx, callback) {
    var self = this;
    var params = [];
    params.push(tx.hash);
    var requestBody = {jsonrpc: '2.0', id: 2, method: "eth_getTransactionByHash", params: params};
    self.rpcRequest_(requestBody, function(error, result) {
        if(!error) {
            var value = web3.toDecimal(result.result.value);
            value = Number(web3.fromWei(value, "ether"));
            var memo = result.result.input;
            if (memo && 12 == memo.length) {
                memo = memo.substr(2); 
            }
            var cctx = new Object({txid: result.result.hash, inputAddr: result.result.from, 
                outputAddr: result.result.to, amount: value, memo: memo});
            //console.log("eth_getTransactionByHash cctx: %j", cctx);
            callback(null, cctx);
        } else {
            console.log("eth_getTransactionByHash error: %j", error);
            callback("error", null);
        }
    });
};

CryptoProxy.prototype.completeTransactions_ = function(blockInfo, callback) {
    var self = this;
    var index = new Object({id: blockInfo.hash, height: blockInfo.index});
    var prevIndex = new Object({id: blockInfo.preHash, height: blockInfo.prevIndex});
    if (blockInfo.txs.length > 0) {
        Async.map(blockInfo.txs, self.getCCTxByTxHash_.bind(self), function(error, results) {
            if(!error) {
                var txs = [];
                if (results.length) {
                    for (var i = 0; i < results.length; i++) {
                        if (results[i].inputAddr == self.hotAccount || results[i].outputAddr == self.hotAccount) {
                            txs.push(results[i]);
                        }
                    }
                }
                var block = new Object({index: index, prevIndex: prevIndex, txs: txs});
                callback(null, block);
            } else {
                console.error("completeTransactions_ error: " + error);
                callback(error, null);
            }
        });
    } else {
        var block = new Object({index: index, prevIndex: prevIndex,
            txs: []});
        callback(null, block);

    }
}

CryptoProxy.prototype.getBlockHash_ = function(height, callback) {
    var self = this;
    var params = [];
    params.push(height);
    var flag = true;
    params.push(flag);
    var requestBody = {jsonrpc: '2.0', id: 2, method: "eth_getBlockByNumber", params: params};
    //console.log("getBlockHash_ request: ", requestBody);
    self.rpcRequest_(requestBody, function(error, result) {
        if(!error) {
            //console.log("getBlockHash_ result: ", result);
            callback(null, result.result.hash);
        } else {
            console.error("getBlockHash_ error: ", error);
            callback(error, null);
        }
    });
};

CryptoProxy.prototype.getBlockByNumber_ = function(height, callback) {
    var self = this;
    var params = [];
    params.push(height);
    params.push(true);
    var requestBody = {jsonrpc: '2.0', id: 2, method: "eth_getBlockByNumber", params: params};
    self.rpcRequest_(requestBody, function(error, result) {
        if(!error && result.result) {
            //console.log("getBlockByNumber_ result: ", result);
            var blockInfo = {};
            blockInfo.index = web3.toDecimal(result.result.number);
            blockInfo.hash = result.result.hash;
            if (blockInfo.index) {
                blockInfo.prevIndex = blockInfo.index - 1;
                blockInfo.preHash = result.result.parentHash;
            } else {
                blockInfo.prevIndex = 0;
                blockInfo.preHash = "";
            }
            blockInfo.txs = result.result.transactions;
            callback(null, blockInfo);
        } else {
            console.error("getBlockByNumber_ error: ", error);
            if (error) {
                callback(error, null);
            } else {
                callback("nothing", null);
            }
        }
    });
};

CryptoProxy.invokeCallback_ = function(error, resultFun, callback) {
    if (error) {
        callback(error);
    } else {
        callback(null, resultFun());
    }
};
