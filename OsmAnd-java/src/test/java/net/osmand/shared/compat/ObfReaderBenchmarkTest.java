package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Timing comparison of {@link net.osmand.shared.binary.BinaryMapIndexReader} against
 * {@link BinaryMapIndexReader}, over the sections the port has brought over so far.
 *
 * An obf file is a handful of sections. The <b>map section</b> is what draws the map: roads,
 * buildings and areas with their geometry and tags, written once per zoom range. The <b>poi
 * section</b> is what search reads: amenities, each a point with a type and its tags. The same
 * thing is often in both - a bakery is a point in the poi section and may be a building outline in
 * the map section - so the two counts below do not add up to "everything in the file", and neither
 * is a subset of the other.
 *
 * <b>Disabled on purpose.</b> The copy is a straight port of java's algorithm, so unlike the
 * collator there is no design decision here to defend; what this answers is what the substitutions
 * cost - the collections that replaced trove, and the strings java interns and the copy does not.
 * The cross platform half, which gives the Kotlin/Native numbers, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/binary/ObfReaderBenchmarkTest.kt over the
 * same files. Neither is a regression gate, and this one asserts only that both readers found the
 * same objects, so a slow machine can never turn the build red. What the copies have to answer is
 * covered by {@link MapSectionCompatTest} and {@link PoiSearchCompatTest}, which do run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*ObfReaderBenchmarkTest" -i
 * </pre>
 *
 * Over the 26 obf files of the tests:
 * <pre>
 * what                                 java      copy     count
 * open the files                     4.3 ms    3.7 ms        26
 * read every map object, all zooms  15.1 ms    9.5 ms     47393
 * read every amenity                 1.9 ms    2.0 ms      6285
 * </pre>
 * Opening is not a like for like: java also reads the address and transport headers, which the
 * copy skips. On the objects the copy is at least as fast as java; why it is faster on the map
 * section was not chased, as nothing in the port depends on the answer. The same files on
 * Kotlin/Native cost 2.4, 22.0 and 3.6 ms, see the cross platform benchmark.
 */
@Ignore("benchmark, run manually")
public class ObfReaderBenchmarkTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	/** One pass over the poi sections is only a couple of milliseconds, too little to time. */
	private static final int POI_PASSES_PER_ROUND = 10;

	@Test
	public void readingObfFiles() throws IOException {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));

		List<File> files = TestObf.files();
		long javaOpen = Long.MAX_VALUE, copyOpen = Long.MAX_VALUE;
		long javaMap = Long.MAX_VALUE, copyMap = Long.MAX_VALUE;
		long javaPoi = Long.MAX_VALUE, copyPoi = Long.MAX_VALUE;
		int mapObjects = 0;
		int amenities = 0;

		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			List<BinaryMapIndexReader> java = new ArrayList<>();
			for (File f : files) {
				java.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
			}
			long javaOpenRound = System.nanoTime() - started;

			started = System.nanoTime();
			List<net.osmand.shared.binary.BinaryMapIndexReader> copy = new ArrayList<>();
			for (File f : files) {
				copy.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
			}
			long copyOpenRound = System.nanoTime() - started;

			started = System.nanoTime();
			int javaMapRead = mapObjectsJava(java);
			long javaMapRound = System.nanoTime() - started;

			started = System.nanoTime();
			int copyMapRead = mapObjectsCopy(copy);
			long copyMapRound = System.nanoTime() - started;

			started = System.nanoTime();
			int javaPoiRead = 0;
			for (int pass = 0; pass < POI_PASSES_PER_ROUND; pass++) {
				javaPoiRead = amenitiesJava(java);
			}
			long javaPoiRound = (System.nanoTime() - started) / POI_PASSES_PER_ROUND;

			started = System.nanoTime();
			int copyPoiRead = 0;
			for (int pass = 0; pass < POI_PASSES_PER_ROUND; pass++) {
				copyPoiRead = amenitiesCopy(copy);
			}
			long copyPoiRound = (System.nanoTime() - started) / POI_PASSES_PER_ROUND;

			for (BinaryMapIndexReader r : java) {
				r.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader r : copy) {
				r.close();
			}

			assertEquals("map objects", javaMapRead, copyMapRead);
			assertEquals("amenities", javaPoiRead, copyPoiRead);
			if (round >= WARMUP_ROUNDS) {
				javaOpen = Math.min(javaOpen, javaOpenRound);
				copyOpen = Math.min(copyOpen, copyOpenRound);
				javaMap = Math.min(javaMap, javaMapRound);
				copyMap = Math.min(copyMap, copyMapRound);
				javaPoi = Math.min(javaPoi, javaPoiRound);
				copyPoi = Math.min(copyPoi, copyPoiRound);
				mapObjects = javaMapRead;
				amenities = javaPoiRead;
			}
		}

		System.out.println();
		System.out.printf("%-34s %9s %9s %9s%n", "what", "java", "copy", "count");
		row("open the files", javaOpen, copyOpen, files.size());
		row("read every map object, all zooms", javaMap, copyMap, mapObjects);
		row("read every amenity", javaPoi, copyPoi, amenities);
		System.out.println();
	}

	private static void row(String what, long java, long copy, int count) {
		System.out.printf("%-34s %6.1f ms %6.1f ms %9d%n", what, java / 1e6, copy / 1e6, count);
	}

	private int mapObjectsJava(List<BinaryMapIndexReader> readers) throws IOException {
		int read = 0;
		for (BinaryMapIndexReader reader : readers) {
			for (int zoom : mapZoomsJava(reader)) {
				read += reader.searchMapIndex(BinaryMapIndexReader.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, null)).size();
			}
		}
		return read;
	}

	private int mapObjectsCopy(List<net.osmand.shared.binary.BinaryMapIndexReader> readers) {
		int read = 0;
		for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
			for (int zoom : mapZoomsCopy(reader)) {
				read += reader.searchMapIndex(net.osmand.shared.binary.SearchRequest.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, null)).size();
			}
		}
		return read;
	}

	private int amenitiesJava(List<BinaryMapIndexReader> readers) throws IOException {
		int read = 0;
		for (BinaryMapIndexReader reader : readers) {
			List<Amenity> found = reader.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null));
			read += found.size();
		}
		return read;
	}

	private int amenitiesCopy(List<net.osmand.shared.binary.BinaryMapIndexReader> readers) {
		int read = 0;
		for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
			read += reader.searchPoi(net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null, null)).size();
		}
		return read;
	}

	/** Every zoom the file has a map level for, so that the whole section is read. */
	private static List<Integer> mapZoomsJava(BinaryMapIndexReader reader) {
		List<Integer> zooms = new ArrayList<>();
		for (MapIndex index : reader.getMapIndexes()) {
			for (MapRoot root : index.getRoots()) {
				if (!zooms.contains(root.getMinZoom())) {
					zooms.add(root.getMinZoom());
				}
			}
		}
		return zooms;
	}

	private static List<Integer> mapZoomsCopy(net.osmand.shared.binary.BinaryMapIndexReader reader) {
		List<Integer> zooms = new ArrayList<>();
		for (net.osmand.shared.binary.MapIndex index : reader.getMapIndexes()) {
			for (net.osmand.shared.binary.MapRoot root : index.getRoots()) {
				if (!zooms.contains(root.getMinZoom())) {
					zooms.add(root.getMinZoom());
				}
			}
		}
		return zooms;
	}
}
