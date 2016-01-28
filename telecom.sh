#!/bin/sh
killall -9 java
cd bin
java cc.Telecom ../tests/proplognorm/conf/telecoms.conf -c ../tests/proplognorm/conf/telecom$1.conf
