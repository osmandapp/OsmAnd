#!/bin/bash

if [ ! -d bin ];
 then
    echo "Building OsmAnd-java...";
    ant build
fi
echo "Starting tests..."
ant junits
echo "Results can be found in result.txt file."