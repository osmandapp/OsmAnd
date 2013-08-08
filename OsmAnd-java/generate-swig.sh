#!/bin/bash

SRCLOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

rm -rf "$SRCLOC/src/net/osmand/core/jni"
mkdir -p "$SRCLOC/src/net/osmand/core/jni"

rm -rf "$SRCLOC/c-src"
mkdir -p "$SRCLOC/c-src"

swig -java -package net.osmand.core.jni -outdir "$SRCLOC/src/net/osmand/core/jni" -o "$SRCLOC/c-src/swig.cpp" -I"$SRCLOC/../../core/include" -c++ -v "$SRCLOC/../../core/core.swig"
