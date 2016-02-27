#!/bin/bash
cd src
javac -cp ../lib/commons-math3-3.6/commons-math3-3.6.jar -d ../bin */*java
cd ..
echo "Done."
