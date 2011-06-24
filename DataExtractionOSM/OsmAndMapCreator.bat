REM the JVM. With the below settings the heap size (Available memory for the application)
REM will range from 64 megabyte up to 720 megabyte.

start javaw.exe -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx720M -cp "./OsmAndMapCreator.jar;./lib/*.jar" net.osmand.swing.OsmExtractionUI