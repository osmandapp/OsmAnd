package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapIndexReader;
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
 * Timing comparison of the poi section of {@link net.osmand.shared.binary.BinaryMapIndexReader}
 * against {@link BinaryMapIndexReader}.
 *
 * <b>Disabled on purpose.</b> The copy is a straight port of java's algorithm; what this answers
 * is what the substitutions cost - the collections that replaced trove, and the strings java
 * interns and the copy does not, of which the poi section has many, since every category, subtype
 * and tag group value goes through them. The cross platform half, which gives the Kotlin/Native
 * number, is the poi row of
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/binary/MapReaderBenchmarkTest.kt over the
 * same files. Neither is a regression gate, and this one asserts only that both readers found the
 * same amenities. What the copy has to answer is covered by {@link PoiSearchCompatTest}.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*PoiSearchBenchmarkTest" -i
 * </pre>
 *
 * Reading every amenity of the 26 obf files of the tests:
 * <pre>
 * poi       java   2.1 ms   copy   2.3 ms   6285 amenities
 * </pre>
 * The two are the same on the jvm; the strings java interns turn out not to pay for themselves
 * here. On Kotlin/Native the same corpus costs 3.5 ms, half again as much rather than the double
 * the map section costs, see the cross platform benchmark.
 */
@Ignore("benchmark, run manually")
public class PoiSearchBenchmarkTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	/** One pass over the files is only a few milliseconds, too little to time on the jvm. */
	private static final int PASSES_PER_ROUND = 10;

	@Test
	public void readingAmenities() throws IOException {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));

		List<File> files = TestObf.files();
		long java = Long.MAX_VALUE;
		long copy = Long.MAX_VALUE;
		int amenities = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			List<BinaryMapIndexReader> javaReaders = new ArrayList<>();
			List<net.osmand.shared.binary.BinaryMapIndexReader> copyReaders = new ArrayList<>();
			for (File f : files) {
				javaReaders.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
				copyReaders.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
			}

			long started = System.nanoTime();
			int javaRead = 0;
			for (int pass = 0; pass < PASSES_PER_ROUND; pass++) {
				javaRead = searchJava(javaReaders);
			}
			long javaRound = (System.nanoTime() - started) / PASSES_PER_ROUND;

			started = System.nanoTime();
			int copyRead = 0;
			for (int pass = 0; pass < PASSES_PER_ROUND; pass++) {
				copyRead = searchCopy(copyReaders);
			}
			long copyRound = (System.nanoTime() - started) / PASSES_PER_ROUND;

			for (BinaryMapIndexReader r : javaReaders) {
				r.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader r : copyReaders) {
				r.close();
			}

			assertEquals("amenities", javaRead, copyRead);
			if (round >= WARMUP_ROUNDS) {
				java = Math.min(java, javaRound);
				copy = Math.min(copy, copyRound);
				amenities = javaRead;
			}
		}
		System.out.printf("poi       java %5.1f ms   copy %5.1f ms   %d amenities%n",
				java / 1e6, copy / 1e6, amenities);
	}

	private int searchJava(List<BinaryMapIndexReader> readers) throws IOException {
		int read = 0;
		for (BinaryMapIndexReader reader : readers) {
			List<Amenity> found = reader.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null));
			read += found.size();
		}
		return read;
	}

	private int searchCopy(List<net.osmand.shared.binary.BinaryMapIndexReader> readers) {
		int read = 0;
		for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
			read += reader.searchPoi(net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null, null)).size();
		}
		return read;
	}
}
