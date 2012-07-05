#!/bin/sh

# This file will start the Osm Extraction UI with custom memory settings for
# the JVM. With the below settings the heap size (Available memory for the application)
# will range from 64 megabyte up to 512 megabyte.

# Pass full paths to route_tests.xml files. All routing.xml configuration should be configured by OsmAndMapCreator  
java -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx512M -cp "./OsmAndMapCreator.jar:./lib/*.jar" net.osmand.router.test.RouterTestsSuite $@
