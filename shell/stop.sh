ps -ef | grep 'coinport-admin' | grep -v 'grep' | awk '{print $2}' | xargs kill
