cd /var/coinport/admin/coinport-admin-*/bin/
#nohup ./coinport-admin -Dhttp.port=9090 -Dhttp.address=172.31.1.67 -Dakka.config=akka-prod.conf &
nohup ./coinport-admin -Dhttp.port=9090 -Dakka.config=akka-prod.conf &
