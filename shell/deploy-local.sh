cd /coinport/admin/exchange
git fetch && git rebase origin/master
./activator clean dist
cd target/universal/
unzip coinport-admin-*
cd coinport-admin-*/bin/
nohup ./coinport-admin &
