#!/bin/sh

MONGO_HOME="/usr/local/klzhong/midware/mongodb-osx-x86_64-2.6.0"
HOST="127.0.0.1"
PORT="27017"

cd $MONGO_HOME

bin/mongo --host $HOST:$PORT <<EOF
use coinex_readers
db.user_profiles.find({"data.mobileVerified" : true})
EOF
