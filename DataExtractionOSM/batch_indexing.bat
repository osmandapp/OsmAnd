REM the JVM. With the below settings the heap size (Available memory for the application)
REM will range from 64 megabyte up to 512 megabyte.


start java.exe -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx512M -cp "./OsmAndMapCreator.jar;./lib/*.jar" net.osmand.data.index.IndexBatchCreator ./batch.xml