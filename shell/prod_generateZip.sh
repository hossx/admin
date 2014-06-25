cd /var/coinport/code/admin/
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
