#!/bin/sh

# This file will start the Osm Extraction UI with custom memory settings for
# the JVM. With the below settings the heap size (Available memory for the application)
# will range from 64 megabyte up to 512 megabyte.

java -jar OsmAndMapCreator.jar -Xms64M -Xmx512M