#!/bin/sh
cd bin
for i in 10 20 100 200 1000 2000 10000 20000 100000 200000
do
for l in 2 3
do
for d in 25 50 75 100 150 200 500
do
for x in 1 2 3
do
java nocrypto.LeaderAgency ../tests/pokec/conf/agencies.conf -c ../tests/pokec/conf/agency1.conf -d ${d} -i ${i} -l ${l} -q
sleep 1
done
done
done
done
