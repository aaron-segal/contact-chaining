#!/bin/sh
killall -9 java
cd bin
java nocrypto.Telecom ../tests/pokec/conf/telecoms.conf -c ../tests/pokec/conf/telecom$1.conf
