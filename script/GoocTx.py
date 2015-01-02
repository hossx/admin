#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2014 Coinport Inc. All Rights Reserved.

__author__ = 'c@coinport.com (Ma Chao)'

class GoocTx:
    def __init__(self, tx):
        self.txid = tx['transactionId']
        self.comment = tx['comment']
        self.senderAdd = tx['addressA']
        self.receiverAdd = tx['addressB']
        self.senderPhone = tx['phoneA']
        self.receiverPhone = tx['phoneB']
        self.amount = tx['coins']
        self.time = tx['confirmTime']
        self.txtype = tx['transactionType']
