#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2014 Coinport Inc. All Rights Reserved.

__author__ = 'c@coinport.com (Ma Chao)'

class GoocTx:
    def __init__(self, tx):
        self._id = tx['transactionId']
        self.c = tx['comment']
        self.sa = tx['addressA']
        self.ra = tx['addressB']
        self.sp = tx['phoneA']
        self.rp = tx['phoneB']
        self.a = tx['coins']
        self.t = tx['confirmTime']
        self.tt = tx['transactionType']
        self.cps = 'PENDING'
