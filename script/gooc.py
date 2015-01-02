#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2014 Coinport Inc. All Rights Reserved.

import json
import pymongo
import urllib
import urllib2
import sys

__author__ = 'c@coinport.com (Ma Chao)'
MAXINT = sys.maxint

client = pymongo.MongoClient('localhost', 27017)
db = client['gooc']
collection = db['txs']
cursor = db['cursor']

def getGoocTxJson(minTxid, maxTxid):
    requrl = 'http://localhost:9000/GooCoin/rest/tx/partner/history'
    reqdata = { 'minId':minTxid, 'maxId':maxTxid, 'pageSize':20, 'address':'1DuwDsv2W3v5XyXT7RAvwtFHtnSnWveHse'}
    headerdata = {'Content-Type':'application/json'}

    req = urllib2.Request(url = requrl, headers=headerdata, data =json.dumps(reqdata))
    try:
        res = urllib2.urlopen(req, timeout=1)
    except:
        return None
    return res.read()

def getFakeGoocTxJson():
    inf = open('gooc_tx')
    res = inf.read()
    inf.close()
    return res

def fetchNewGoocTx(currentMinTxid, lastTxId):
    minTxid = currentMinTxid
    meetLastTx = False
    res = getFakeGoocTxJson()
    # res = getGoocTxJson(lastTxId, currentMinTxid)
    if (res == None):
        return None
    goocResponse = json.loads(res)
    if (goocResponse == None or goocResponse['status'] != 'OK'):
        return (None, False, minTxid)
    goocTxs = goocResponse['records']
    retTxs = []
    for tx in goocTxs:
        txid = tx['transactionId']
        if (txid > lastTxId + 1):
            retTxs.append(GoocTx(tx))
        elif (txid == lastTxId + 1):
            meetLastTx = True
        minTxid = min(minTxid, txid)
    if (lastTxId == -1):
            meetLastTx = True
    return (retTxs, meetLastTx, minTxid)

def saveNewGoocTx(txs):
    if (not len(txs) > 0):
        return
    # cursor.update({"_id":0}, {"_id": 0, "txid": 213591975}, True)

def fetchNewTxsSinceLastFetch():
    txCursor = cursor.find_one()
    lastTxId = txCursor['txid']
    if (lastTxId == None):
        lastTxId = 0
    (newGoocTxs, meetLastTx, currentMinTxid) = fetchNewGoocTx(MAXINT, lastTxId - 1)
    saveNewGoocTx(newGoocTxs)
    while (not meetLastTx):
        (newGoocTxs, meetLastTx, currentMinTxid) = fetchNewGoocTx(currentMinTxid, lastTxId -1 )
        saveNewGoocTx(newGoocTxs)

def main():
    fetchNewTxsSinceLastFetch()

if __name__ == '__main__':
    main()
