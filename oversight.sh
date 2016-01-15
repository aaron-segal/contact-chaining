#!/bin/sh
cd bin
for l in 2 3
do
for d in 25 50 75 100 150 200 500
do
for x in 1 2 3
do
java cc.OversightAgency ../tests/lognorm/conf/agencies.conf -c ../tests/lognorm/conf/agency$1.conf -d ${d} -l ${l} -q
sleep 1
done
done
done
