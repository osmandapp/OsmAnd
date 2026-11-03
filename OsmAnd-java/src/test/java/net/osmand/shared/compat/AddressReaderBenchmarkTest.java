package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.shared.api.KStringMatcherMode;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Timing of the address section in {@link net.osmand.shared.binary.BinaryMapIndexReader} against
 * {@link BinaryMapIndexReader}: opening the files, which now walks the address header too, and then
 * every settlement, every street and every house, and a search by name.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_ADDRESS_BENCHMARK} is
 * set. The files are {@link TestObf#files()}, so {@code OSMAND_OBF_CORPUS} points it at real maps;
 * the ones the tests ship with are too small to time. It asserts only that both readers found the
 * same number of objects, so a slow machine never turns the build red; what they find is compared
 * object by object in {@link AddressReaderCompatTest}.
 * <pre>
 * OSMAND_ADDRESS_BENCHMARK=1 OSMAND_TEST_HEAP=4g OSMAND_OBF_CORPUS=/maps/Slovakia_europe.obf \
 *   ./gradlew :OsmAnd-java:test --tests "*AddressReaderBenchmarkTest" --rerun -i
 * </pre>
 *
 * On three regional maps, best of five in ms; the native column is the cross platform half in
 * OsmAnd-shared, {@code AddressReaderBenchmarkTest.kt}, on the release simulator binary:
 * <pre>
 * what                         Noord-Holland (395 MB)     Kyiv (204 MB)          Slovakia (754 MB)
 *                              java  copy  native   java  copy  native   java  copy  native
 * open the file                12.6   7.1     6.0    8.5   4.3     2.7   38.4  20.4    13.5
 * every settlement             28.6  21.9    24.5    3.0   2.2     2.8   11.3   6.6     7.3
 * every street                 67.5  51.6    71.2   17.6  14.8    24.7   58.7  47.1    60.8
 * every house                   494   409     687   54.7  49.8    96.2    394   314     591
 * find by name, 20 queries     23.2   7.6    13.5   25.1   8.7    15.0   23.0   9.4    19.1
 * </pre>
 * settlements, streets, houses, found: Noord-Holland 75 587, 122 172, 4 268 244, 11 624; Kyiv 2 910,
 * 28 186, 177 312, 969; Slovakia 9 055, 85 483, 3 582 428, 1 487.
 *
 * The copy on the jvm is faster than java in every phase, 0.32 to 0.91 of its time; the name search
 * gains most, from the collation key of {@code KCollatorStringMatcher} and from the walk over the
 * string table of the name index preparing each key once. Opening costs the copy the same as before
 * the address header was read: 7.02, 4.29 and 20.11 ms on master. Kotlin/Native reads settlements
 * at about the jvm's speed, streets and houses at 1.3 to 1.9 times the copy on the jvm, and searches
 * by name at 1.7 to 2.0 times, still below java on the jvm. Before the string table walk prepared
 * its keys the native name search took 17.8, 35.6 and 52.4 ms.
 */
public class AddressReaderBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	private static final int QUERIES = 20;
	private static final int OPEN_PASSES_PER_ROUND = 10;

	@Test
	public void readingAddresses() throws IOException {
		Assume.assumeTrue("set OSMAND_ADDRESS_BENCHMARK to run", System.getenv("OSMAND_ADDRESS_BENCHMARK") != null);
		for (File file : TestObf.files()) {
			benchmark(file);
		}
	}

	private void benchmark(File file) throws IOException {
		List<String> queries = queries(file);
		long[] best = new long[10];
		java.util.Arrays.fill(best, Long.MAX_VALUE);
		int cities = 0, streets = 0, houses = 0, found = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			for (int i = 0; i < OPEN_PASSES_PER_ROUND; i++) {
				new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file).close();
			}
			long javaOpen = (System.nanoTime() - started) / OPEN_PASSES_PER_ROUND;
			started = System.nanoTime();
			for (int i = 0; i < OPEN_PASSES_PER_ROUND; i++) {
				new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath()).close();
			}
			long copyOpen = (System.nanoTime() - started) / OPEN_PASSES_PER_ROUND;

			BinaryMapIndexReader java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
			net.osmand.shared.binary.BinaryMapIndexReader copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());

			started = System.nanoTime();
			List<City> jcities = new ArrayList<>();
			for (CityBlocks type : CityBlocks.allTypes()) {
				jcities.addAll(java.getCities(null, type));
			}
			long javaCities = System.nanoTime() - started;
			started = System.nanoTime();
			List<net.osmand.shared.data.City> kcities = new ArrayList<>();
			for (CityBlocks type : CityBlocks.allTypes()) {
				kcities.addAll(copy.getCities(null, net.osmand.shared.binary.CityBlocks.valueOf(type.name())));
			}
			long copyCities = System.nanoTime() - started;
			assertEquals("settlements", jcities.size(), kcities.size());

			started = System.nanoTime();
			int jstreets = 0;
			for (City c : jcities) {
				java.preloadStreets(c, null, null);
				jstreets += c.getStreets().size();
			}
			long javaStreets = System.nanoTime() - started;
			started = System.nanoTime();
			int kstreets = 0;
			for (net.osmand.shared.data.City c : kcities) {
				copy.preloadStreets(c, null);
				kstreets += c.getStreets().size();
			}
			long copyStreets = System.nanoTime() - started;
			assertEquals("streets", jstreets, kstreets);

			started = System.nanoTime();
			int jhouses = 0;
			for (City c : jcities) {
				for (Street s : c.getStreets()) {
					java.preloadBuildings(s, null, null);
					jhouses += s.getBuildings().size();
				}
			}
			long javaHouses = System.nanoTime() - started;
			started = System.nanoTime();
			int khouses = 0;
			for (net.osmand.shared.data.City c : kcities) {
				for (net.osmand.shared.data.Street s : c.getStreets()) {
					copy.preloadBuildings(s, null);
					khouses += s.getBuildings().size();
				}
			}
			long copyHouses = System.nanoTime() - started;
			assertEquals("houses", jhouses, khouses);

			started = System.nanoTime();
			int jfound = 0;
			for (String query : queries) {
				List<MapObject> res = java.searchAddressDataByName(BinaryMapIndexReader.buildAddressByNameRequest(
						null, query, StringMatcherMode.CHECK_STARTS_FROM_SPACE));
				jfound += res.size();
			}
			long javaSearch = System.nanoTime() - started;
			started = System.nanoTime();
			int kfound = 0;
			for (String query : queries) {
				kfound += copy.searchAddressDataByName(net.osmand.shared.binary.SearchRequest.buildAddressByNameRequest(
						null, query, KStringMatcherMode.CHECK_STARTS_FROM_SPACE)).size();
			}
			long copySearch = System.nanoTime() - started;
			assertEquals("found by name", jfound, kfound);

			java.close();
			copy.close();
			if (round >= WARMUP_ROUNDS) {
				long[] times = {javaOpen, copyOpen, javaCities, copyCities, javaStreets, copyStreets,
						javaHouses, copyHouses, javaSearch, copySearch};
				for (int i = 0; i < times.length; i++) {
					best[i] = Math.min(best[i], times[i]);
				}
				cities = jcities.size();
				streets = jstreets;
				houses = jhouses;
				found = jfound;
			}
		}
		System.out.println();
		System.out.println("### " + file.getName() + ", " + (file.length() >> 20) + " MB, best of " + MEASURED_ROUNDS);
		System.out.printf("%-34s %10s %10s %6s %9s%n", "what", "java", "copy", "x", "count");
		row("open the file", best[0], best[1], 1);
		row("read every settlement", best[2], best[3], cities);
		row("read every street", best[4], best[5], streets);
		row("read every house", best[6], best[7], houses);
		row("find by name (" + queries.size() + " queries)", best[8], best[9], found);
	}

	private static void row(String what, long java, long copy, int count) {
		System.out.printf("%-34s %7.1f ms %7.1f ms %6.2f %9d%n", what, java / 1e6, copy / 1e6, (double) copy / java, count);
	}

	/** Names the file holds, lowercased as the search hands them over, and their first three letters. */
	private static List<String> queries(File file) throws IOException {
		BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		List<String> names = new ArrayList<>();
		for (CityBlocks type : CityBlocks.allTypes()) {
			for (City c : reader.getCities(null, type)) {
				names.add(c.getName());
			}
		}
		Set<String> queries = new LinkedHashSet<>();
		int step = Math.max(1, names.size() / (QUERIES / 2));
		for (int i = 0; i < names.size() && queries.size() < QUERIES; i += step) {
			String name = names.get(i).trim().toLowerCase();
			if (name.length() > 3) {
				queries.add(name);
				queries.add(name.substring(0, 3));
			}
		}
		reader.close();
		return new ArrayList<>(queries);
	}
}
