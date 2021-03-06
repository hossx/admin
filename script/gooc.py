#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2014 Coinport Inc. All Rights Reserved.

import json
import pymongo
import urllib
import urllib2
import sys
import time
from GoocTx import GoocTx

__author__ = 'c@coinport.com (Ma Chao)'

MAXINT = sys.maxint
MAXRETRY = 3

client = pymongo.MongoClient('localhost', 27017)
db = client['gooc']
dTxsC = db['dTxs']
cursorC = db['cursor']

def getGoocDepositTxs(minTxid, maxTxid):
    time.sleep(2)

    requrl = 'http://goocoin.com:8080/GooCoin/rest/tx/partner/history'
    reqdata = { 'minId':minTxid, 'maxId':maxTxid, 'pageSize':20, 'address':'1DuwDsv2W3v5XyXT7RAvwtFHtnSnWveHse'}
    headerdata = {'Content-Type':'application/json'}

    req = urllib2.Request(url = requrl, headers=headerdata, data =json.dumps(reqdata))
    res = None
    try:
        res = urllib2.urlopen(req, timeout = 10)
        resStr = res.read()
        res.close()
        print resStr;
        sys.stdout.flush()
        if (resStr == None):
            return None
        response = json.loads(resStr)
        if (response == None or response['status'] != 'OK'):
            return None
        return response['records']
    except:
        print "netwook error";
        sys.stdout.flush()
        if (not res == None):
            res.close()
        return None

def getFakeGoocDepositTxs(minTxid, maxTxid):
    batchNum = 4
    inf = open('gooc_tx')
    res = inf.read()
    inf.close()
    response = json.loads(res)
    if (response == None or response['status'] != 'OK'):
        return None
    txs = response['records']
    retTxs = []
    for tx in txs:
        if (tx['transactionId'] > minTxid and tx['transactionId'] < maxTxid):
            retTxs.append(tx)
        if (len(retTxs) == batchNum):
            break;
    return retTxs

def fetchNewGoocTx(currentMinTxid, lastTxId):
    minTxid = currentMinTxid
    meetLastTx = False
    # goocTxs = getFakeGoocDepositTxs(lastTxId, currentMinTxid)
    goocTxs = getGoocDepositTxs(lastTxId, currentMinTxid)
    if (goocTxs == None):
        return (None, False, minTxid)
    retTxs = []
    for tx in goocTxs:
        gtx = GoocTx(tx)
        if (gtx._id > lastTxId + 1):
            retTxs.append(gtx)
        elif (gtx._id == lastTxId + 1):
            meetLastTx = True
        minTxid = min(minTxid, gtx._id)
    if (lastTxId == -1):
        meetLastTx = True
    return (retTxs, meetLastTx, minTxid)

def saveNewGoocTx(txs):
    if (txs == None or not len(txs) > 0):
        return
    for tx in txs:
        try:
            dTxsC.insert(tx.__dict__)
        except:
            print 'try to insert exists item: ', tx.__dict__;
            sys.stdout.flush()

def getMaxTxid(txs):
    if (txs == None):
        return 0
    maxId = 0
    for tx in txs:
        maxId = max(maxId, tx._id)
    return maxId


def fetchNewTxsSinceLastFetch():
    txCursor = cursorC.find_one()
    if (txCursor == None):
        lastTxId = 0
    else:
        lastTxId = txCursor['txid']
    if (lastTxId == None):
        lastTxId = 0
    maxId = lastTxId
    retryTimes = 0
    (newGoocTxs, meetLastTx, currentMinTxid) = fetchNewGoocTx(MAXINT, lastTxId - 1)
    saveNewGoocTx(newGoocTxs)
    if (newGoocTxs == None or (len(newGoocTxs) == 0 and not meetLastTx)):
        retryTimes += 1
    maxId = max(maxId, getMaxTxid(newGoocTxs))
    while (not meetLastTx):
        (newGoocTxs, meetLastTx, currentMinTxid) = fetchNewGoocTx(currentMinTxid, lastTxId - 1)
        saveNewGoocTx(newGoocTxs)
        if (newGoocTxs == None or (len(newGoocTxs) == 0 and not meetLastTx)):
            retryTimes += 1
            if (retryTimes == MAXRETRY):
                print 'can not get txs between %d - %d'%(currentMinTxid, lastTxId);
                sys.stdout.flush()
                break;
        else:
            retryTimes = 0
        maxId = max(maxId, getMaxTxid(newGoocTxs))
    cursorC.update({"_id":0}, {"_id": 0, "txid": maxId}, True)

def main():
    while True:
        print 'fetching latest deposit txs...';
        sys.stdout.flush()
        fetchNewTxsSinceLastFetch()
        print 'finished';
        sys.stdout.flush()
        time.sleep(10)

if __name__ == '__main__':
    main()
