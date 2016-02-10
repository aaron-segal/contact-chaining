#!/bin/sh
killall -9 java
cd bin
java cc.Telecom ../tests/pokec/conf/telecoms.conf -c ../tests/pokec/conf/telecom$1.conf
