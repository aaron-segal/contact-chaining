#!/bin/bash
cd src
javac -cp ../lib/commons-math3-3.6/commons-math3-3.6.jar cc/*java
mv -f cc/*class ../bin/cc
cd ..
echo "Done."
