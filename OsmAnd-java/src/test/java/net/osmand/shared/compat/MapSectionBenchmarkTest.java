package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Timing comparison of the map section of {@link net.osmand.shared.binary.BinaryMapIndexReader}
 * against {@link BinaryMapIndexReader}.
 *
 * <b>Disabled on purpose.</b> The copy is a straight port of java's algorithm, so unlike the
 * collator there is no design decision here to defend; what this answers is the cost of the
 * collections that replaced trove and of the strings java interns and the copy does not. The
 * cross platform half, which gives the Kotlin/Native number, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/binary/MapReaderBenchmarkTest.kt over the
 * same files and the same zooms. Neither is a regression gate, and this one asserts only that
 * both readers found the same objects, so a slow machine can never turn the build red. What the
 * copy has to answer is covered by {@link MapSectionCompatTest}, which does run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*MapSectionBenchmarkTest" -i
 * </pre>
 *
 * Over the 26 obf files of the tests, reading every object of every zoom level once:
 * <pre>
 * open      java   4.6 ms   copy   3.8 ms
 * search    java  15.8 ms   copy   9.5 ms   47393 objects, 0.33 us against 0.20 us each
 * </pre>
 * The open column is not a like for like: java also reads the address, poi and transport headers,
 * which the copy skips. On the objects the copy is the faster of the two, which is all the port
 * needs; why it is faster was not chased, as nothing depends on it. The same corpus on
 * Kotlin/Native costs 2.4 and 22.0 ms, 0.46 us an object, see the cross platform benchmark.
 */
@Ignore("benchmark, run manually")
public class MapSectionBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void readingMapObjects() throws IOException {
		List<File> files = TestObf.files();
		System.out.println("files: " + files.size());

		long javaOpen = Long.MAX_VALUE;
		long copyOpen = Long.MAX_VALUE;
		long javaSearch = Long.MAX_VALUE;
		long copySearch = Long.MAX_VALUE;
		int javaObjects = 0;
		int copyObjects = 0;
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
			int javaRead = searchJava(java);
			long javaSearchRound = System.nanoTime() - started;

			started = System.nanoTime();
			int copyRead = searchCopy(copy);
			long copySearchRound = System.nanoTime() - started;

			for (BinaryMapIndexReader r : java) {
				r.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader r : copy) {
				r.close();
			}

			assertEquals("objects", javaRead, copyRead);
			if (round >= WARMUP_ROUNDS) {
				javaOpen = Math.min(javaOpen, javaOpenRound);
				copyOpen = Math.min(copyOpen, copyOpenRound);
				javaSearch = Math.min(javaSearch, javaSearchRound);
				copySearch = Math.min(copySearch, copySearchRound);
				javaObjects = javaRead;
				copyObjects = copyRead;
			}
		}
		System.out.printf("open      java %5.1f ms   copy %5.1f ms%n", javaOpen / 1e6, copyOpen / 1e6);
		System.out.printf("search    java %5.1f ms   copy %5.1f ms   %d objects%n",
				javaSearch / 1e6, copySearch / 1e6, javaObjects);
		assertEquals(javaObjects, copyObjects);
	}

	private int searchJava(List<BinaryMapIndexReader> readers) throws IOException {
		int read = 0;
		for (BinaryMapIndexReader reader : readers) {
			for (int zoom : javaZooms(reader)) {
				read += reader.searchMapIndex(BinaryMapIndexReader.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, null)).size();
			}
		}
		return read;
	}

	private int searchCopy(List<net.osmand.shared.binary.BinaryMapIndexReader> readers) {
		int read = 0;
		for (net.osmand.shared.binary.BinaryMapIndexReader reader : readers) {
			for (int zoom : copyZooms(reader)) {
				read += reader.searchMapIndex(net.osmand.shared.binary.SearchRequest.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, null)).size();
			}
		}
		return read;
	}

	/** Every zoom the file has a level for, so that the whole section is read. */
	private static List<Integer> javaZooms(BinaryMapIndexReader reader) {
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

	private static List<Integer> copyZooms(net.osmand.shared.binary.BinaryMapIndexReader reader) {
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
