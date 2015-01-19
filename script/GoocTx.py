#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2014 Coinport Inc. All Rights Reserved.

__author__ = 'c@coinport.com (Ma Chao)'

import re

cpid = re.compile(r'^100\d{7}$')

class GoocTx:
    def __init__(self, tx):
        self._id = long(tx['transactionId'])
        self.c = tx['comment']
        self.sa = tx['addressA']
        self.ra = tx['addressB']
        self.sp = tx['phoneA']
        self.rp = tx['phoneB']
        self.a = tx['coins']
        self.t = tx['confirmTime']
        self.tt = tx['transactionType']
        if self.a < 1000:
            self.cps = 'UNDER_LIMIT'
        else:
            match = cpid.match(str(self.c))
            if match:
                self.cps = 'PENDING'
            else:
                self.cps = 'BAD_FORM'
        if (self.rp == '15026841984' or self.rp == '50001'):
            self.ty = 'DEPOSIT'
        elif (self.sp == '15026841984' or self.sp == '50001'):
            self.ty = 'WITHDRAWAL'
        else:
            self.ty = 'UNKNOWN'
