#!/bin/sh
cd bin
for i in 1 10 100 1000
do
for l in 2 3
do
for d in 25 50 75 100 150 200 500
do
for x in 1 2 3
do
java cc.LeaderAgency ../tests/pokec/conf/agencies.conf -c ../tests/pokec/conf/agency1.conf -d ${d} -i ${i} -l ${l} -q
sleep 1
done
done
done
done
