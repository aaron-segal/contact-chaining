#!/bin/bash
cd src
mv cc/DataGen*l.java .
javac cc/*java
mv -f cc/*class ../bin/cc
mv *java cc
cd ..
echo "Done."
