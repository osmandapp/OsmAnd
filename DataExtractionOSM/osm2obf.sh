#!/bin/sh

java -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx512M -cp "./OsmAndMapCreator.jar:./lib/*.jar" net.osmand.cli.OsmToObf $@
