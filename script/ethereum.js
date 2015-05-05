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
var  mongodb = require('mongodb');

var  server  = new mongodb.Server('localhost', 27017, {auto_reconnect:true});
var  db = new mongodb.Db('eth', server, {safe:true});

var CryptoProxy = module.exports.CryptoProxy = function(currency, opt_config) {
    Events.EventEmitter.call(this);

    if (opt_config) {
        opt_config.checkInterval != undefined && (this.checkInterval = opt_config.checkInterval);
        opt_config.rpcUrl != undefined && (this.rpcUrl = opt_config.rpcUrl);
    }
    this.currency || (this.currency = currency);

    this.lastIndex = this.currency + '_last_index';
    this.log = Logger.logger(this.currency.toString());
    this.hotAccount = opt_config.hotAccount;
    this.secret = opt_config.secret;
};

CryptoProxy.prototype.logFunction = function log(type) {
    var self = this;
    return function() {
        self.log.info(type, 'ethereum');
    };
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
        url: self.rpcUrl,
        timeout: basicInfo.timeout,
        headers: basicInfo.headers,
        body: JSON.stringify(requestBody)
    }, function (error, response, body) {
        if (!error && response.statusCode == 200 && body) {
            var responseBody = JSON.parse(body);
            callback(null, responseBody);
        } else {
            self.log.error("request_ error", error);
            callback("error", null);
        }
    });
};

CryptoProxy.prototype.convertAmount_ = function(value) {
    return value/100000;
};

CryptoProxy.prototype.start = function() {
    var self = this;
    self.checkBlockAfterDelay_();
};

CryptoProxy.prototype.checkBlockAfterDelay_ = function(opt_interval) {
    var self = this;
    var interval = self.checkInterval;
    opt_interval != undefined && (interval = opt_interval)
    setTimeout(self.checkBlock_.bind(self), interval);
};

CryptoProxy.prototype.constructionTxs = function() {
    var tx = new CryptoCurrencyTransaction({sigId: '1', txid: '1', ids: [],
            inputs: [], outputs: [], status: TransferStatus.COMFIRMING});
    var input = new CryptoCurrencyTransactionPort({address: "b5deb39ddb92d437cc83fab49bb0a5c18c60e33",
            amount: 1000, memo: "1000000001"});
    var output = new CryptoCurrencyTransactionPort({address: "0x76b01dbf75111e85eba70b17cd1abde02885563d",
            amount: 1000, memo: "1000000001"});
    tx.inputs.push(input);
    tx.outputs.push(output);
    var txs = [];
    txs.push(tx);
    return txs;
}

CryptoProxy.prototype.insterTx_ = function(tx) {
    db.open(function(err, db) {
        if (!err) {
            db.collection("txs", {safe:true}, function(error, collection) {
                if (!error) {
                    collection.insert(tx, {safe:true}, function(err, result) {
                        if (err) {
                            console.log(err);
                        } else {
                            console.log(result);
                        }
                    }); 
                } else {
                    console.log("inserDb error: ", error);
                }
            });
        } else {

        }
    });
}

CryptoProxy.prototype.saveLastIndex_ = function(lastIndex) {
    db.open(function(err, db) {
        if (!err) {
            db.collection("cursorC", {safe:true}, function(error, collection) {
                if (!error) {
                    collection.save(lastIndex, {safe:true}, function(err, result) {
                        if (err) {
                            console.log(err);
                        } else {
                            console.log(result);
                        }
                    }); 
                } else {
                    console.log("inserDb error: ", error);
                }
            });
        } else {

        }
    });
}

CryptoProxy.prototype.getLastIndex_ = function(callback) {
    db.open(function(err, db) {
        if (!err) {
            db.collection("cursorC", {safe:true}, function(error, collection) {
                if (!error) {
                    collection.findOne(function(err, result) {
                        if (err) {
                            console.log(err);
                            callback(null, -1);
                        } else {
                            console.log(result);
                            var numIndex = Number(result.index);
                            callback(null, numIndex);
                        }
                    }); 
                } else {
                    console.log("getLastIndex_ error: ", error);
                    callback(null, -1);
                }
            });
        } else {
            callback(err, null);
            callback(null, -1);
        }
    });
}

CryptoProxy.prototype.checkBlock_ = function() {
    var self = this;
    self.getNextCCBlock_(function(error, result){
        if (!error) {
            var blockHight = block.index.hight;
            for (var i = 0; i < block.cctxs.lenght; i++) {
                var tx = {_id: block.cctxs[i].txid, blockNum: blockHight, inputAddr: block.cctxs[i].inputAddr, 
                          outputAddr: block.cctxs[i].outputAddr, amount: block.cctxs[i].amount,
                          memo: amount: block.cctxs[i].memo};
                txs.push(tx);
            }
            Async.map(tsx, self.insertTx_().bind(self), function (error, result) {

            });
            console.log("block: %j", response);
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
    self.log.info("getNextCCBlockSinceLastIndex_ index: ", index);
    self.getBlockCount_(function(error, count) {
        self.log.info("getNextCCBlockSinceLastIndex_ count: ", count);
        if (error) {
            self.log.error(error);
            callback(error);
        } else if (index == count) {
            self.log.debug('no new block found');
            callback('no new block found');
        } else {
            var nextIndex = (index == -1) ? count : index + 1;
            self.log.info("getNextCCBlockSinceLastIndex_ nextIndex: ", nextIndex);
            self.getCCBlockByIndex_(nextIndex, callback);
        }
    });
};

CryptoProxy.prototype.getBlockCount_ = function(callback) {
    var self = this;
    var requestBody = {jsonrpc: '2.0', id: 1, method: "eth_blockNumber", params: []};
    self.log.info("getBlockCount_ request: ", requestBody);
    self.rpcRequest_(requestBody, function(error, result) {
        self.log.info("getBlockCount_ result: ", result);
        if (error) {
            CryptoProxy.invokeCallback_(error, function() {return error}, callback);
        } else {
            var height = web3.toDecimal(result.result);
            CryptoProxy.invokeCallback_(error, function() {return height}, callback);
        }
    });
};

CryptoProxy.prototype.getCCBlockByIndex_ = function(index, callback) {
    var self = this;
    self.log.info("Enter into getCCBlockByIndex_ index: ", index);
    Async.compose(self.completeTransactions_.bind(self),
        self.getBlockByNumber_.bind(self))(index, function(error, block) {
        if (!error) {
            var lastIndex = {_id: self.lastInex, index: index};
            self.saveLastIndex(lastInex, function(error, result) {
                if (!error) {
                    callback(null, block);
                } else {
                    self.log.error("getCCBlockByIndex_error: ", error);
                    callback(error);
                }
            });
        } else {
            self.log.error("getCCBlockByIndex_ error: ", error);
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
            var memo = result.result.input;
            if (memo && 12 == memo.length) {
                memo = memo.substr(2); 
            }
            var cctx = new Object({txid: result.result.hash, inputAddr: result.result.from, 
                outputAddr: result.result.to, amount: value, memo: memo});
            callback(null, cctx);
        } else {
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
                var block = new CryptoCurrencyBlock({index: index, prevIndex: prevIndex,
                    txs: results});
                callback(null, block);
            } else {
                self.log.error("completeTransactions_ error: " + error);
                callback(error, null);
            }
        });
    } else {
        var block = new CryptoCurrencyBlock({index: index, prevIndex: prevIndex,
            txs: []});
        callback(null, block);

    }
}

CryptoProxy.prototype.getBlockHash_ = function(height, callback) {
    var self = this;
    self.log.info("Enter into getBlockHash_ height:", height);
    var params = [];
    params.push(height);
    var flag = true;
    params.push(flag);
    var requestBody = {jsonrpc: '2.0', id: 2, method: "eth_getBlockByNumber", params: params};
    self.log.info("getBlockHash_ request: ", requestBody);
    self.rpcRequest_(requestBody, function(error, result) {
        if(!error) {
            self.log.info("getBlockHash_ result: ", result);
            callback(null, result.result.hash);
        } else {
            self.log.error("getBlockHash_ error: ", error);
            callback(error, null);
        }
    });
};

CryptoProxy.prototype.getBlockByNumber_ = function(height, callback) {
    var self = this;
    self.log.info("Enter into getBlockHash_ height:", height);
    var params = [];
    params.push(height);
    params.push(true);
    var requestBody = {jsonrpc: '2.0', id: 2, method: "eth_getBlockByNumber", params: params};
    self.log.info("getBlockByNumber_ request: ", requestBody);
    self.rpcRequest_(requestBody, function(error, result) {
        if(!error && result.result) {
            self.log.info("getBlockByNumber_ result: ", result);
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
            self.log.error("getBlockByNumber_ error: ", error);
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
