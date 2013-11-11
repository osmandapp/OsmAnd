#!/bin/bash
mkdir -p ../src/com
cp com/google/protobuf/CodedInputStream.java com/google/protobuf/CodedInputStreamRAF.java
cp -Rf com/* ../src/com/
git apply -v protobuf-2.3.patch