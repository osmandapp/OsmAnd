#!/bin/sh

# This file will start the Osm Extraction UI with custom memory settings for
# the JVM. With the below settings the heap size (Available memory for the application)
# will range from 64 megabyte up to 512 megabyte.

java -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx512M -cp "./OsmAndMapCreator.jar:./lib/*.jar" net.osmand.binary.BinaryInspector %1 %2 %3 %4 %5 %6 %7 %8 %9
