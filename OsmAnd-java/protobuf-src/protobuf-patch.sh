#!/bin/bash
mkdir -p ../src/com
cp -Rf com/* ../src/com/
(cd ../ && git apply -v protobuf-src/protobuf-2.3.patch)