#!/bin/bash

if [ ! -d bin ];
 then
    echo "Building OsmAnd-java...";
    ant build
fi
echo "Starting tests..."
ant run-turn-lanes-test
echo "Results can be found in result.txt file."