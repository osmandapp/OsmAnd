#!/bin/bash
# proto 2.4.1
protoc --java_out=OsmAnd-java/src --proto_path=$(pwd)/../resources/protos $(pwd)/../resources/protos/OBF.proto
