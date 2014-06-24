cd /var/coinport/code/admin/exchange
releaseBranch=`git branch -a | grep $1`
if [ -z "$releaseBranch" ];then
  git checkout -b $1 remotes/origin/$1
else
  git checkout $1
fi
./activator clean dist
rm -rf /var/coinport/admin/coinport-admin-*
cp target/universal/coinport-admin-* /var/coinport/admin
cd /var/coinport/admin
unzip coinport-admin-*
cd coinport-admin-*/bin/
nohup ./coinport-admin -Dhttp.port=9090 -Dhttp.address=172.31.1.67 -Dakka.config=akka-prod.conf &
