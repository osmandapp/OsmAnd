package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.MapUtils;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The java half of {@code AmenitySearcherBenchmarkTest} in OsmAnd-shared: how long
 * {@code AmenitySearcher} takes for the amenities {@link AmenitySearcherCompatTest} searched around -
 * to find the amenities around each, to make the object it stands for from itself, from its name and
 * from an object the renderer could have drawn, to search by the first letters of its name, and to
 * merge the amenities of each file. The files and the amenities come from its dump.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_AMENITY_SEARCHER_BENCHMARK}
 * is set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_AMENITY_SEARCHER_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*AmenitySearcherBenchmarkTest" --rerun -i
 * </pre>
 */
public class AmenitySearcherBenchmarkTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/amenity-searcher-java.txt");
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 5;
	private static final String[] KINDS = {"amenities around a place", "object of an amenity", "object of a name",
			"object of a drawn object", "amenities by name", "amenities of a file merged"};

	@Test
	public void benchmarkAmenitySearcher() throws Exception {
		Assume.assumeTrue("set OSMAND_AMENITY_SEARCHER_BENCHMARK to run", System.getenv("OSMAND_AMENITY_SEARCHER_BENCHMARK") != null);
		Assume.assumeTrue("run AmenitySearcherCompatTest first", DUMP.exists());
		Map<String, String> phrases = SearchApisCompatTest.poiPhrases;
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		AmenitySearcher searcher = new AmenitySearcher(MapPoiTypes.getDefault());
		AmenitySearcher.Settings settings = new AmenitySearcher.Settings(() -> "en", () -> false, null);
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		List<int[]> boxes = new ArrayList<>();
		List<List<Amenity>> amenities = new ArrayList<>();
		List<Amenity> samples = new ArrayList<>();
		List<RenderedObject> drawn = new ArrayList<>();
		List<Amenity> prefixed = new ArrayList<>();
		List<String> prefixes = new ArrayList<>();
		try (BufferedReader in = Files.newBufferedReader(DUMP.toPath(), StandardCharsets.UTF_8)) {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				String[] f = line.split("\t");
				switch (f[0]) {
					case "X" -> phrases.put(unhex(f[1]), unhex(f[2]));
					case "F" -> {
						File file = new File(unhex(f[2]));
						BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
						readers.add(reader);
						searcher.addAmenityRepository(file.getName(), new AmenitySearcherCompatTest.JavaRepository(file, reader));
						int[] box = {0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE};
						if (!f[3].equals("-")) {
							String[] b = f[3].split(",");
							for (int i = 0; i < 4; i++) {
								box[i] = Integer.parseInt(b[i]);
							}
						}
						boxes.add(box);
						amenities.add(reader.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(box[0], box[1], box[2], box[3], -1, null, null, null)));
					}
					case "D" -> samples.add(amenities.get(Integer.parseInt(f[1])).get(Integer.parseInt(f[2])));
					case "R" -> drawn.add(BaseDetailsObjectBenchmarkTest.drawn(f[3]));
					case "Q" -> {
						prefixed.add(amenities.get(Integer.parseInt(f[1])).get(Integer.parseInt(f[2])));
						prefixes.add(unhex(f[3]));
					}
					default -> {
					}
				}
			}
		}
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator());
		double[] best = new double[KINDS.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		long[] counts = new long[KINDS.length];
		try {
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long[] nanos = new long[KINDS.length];
				long[] made = new long[KINDS.length];
				long start = System.nanoTime();
				for (Amenity a : samples) {
					made[0] += searcher.searchAmenities(a.getLocation(), settings).size();
				}
				nanos[0] = System.nanoTime() - start;
				start = System.nanoTime();
				for (Amenity a : samples) {
					BaseDetailsObject b = searcher.searchDetailedObject(new AmenitySearcher.Request(a), settings, null);
					made[1] += b == null ? 0 : b.getObjects().size();
				}
				nanos[1] = System.nanoTime() - start;
				start = System.nanoTime();
				for (Amenity a : samples) {
					LatLon l = new LatLon(a.getLocation().getLatitude() + 0.00018, a.getLocation().getLongitude());
					BaseDetailsObject b = searcher.searchDetailedObject(
							new AmenitySearcher.Request(Collections.singletonList(a.getName()), l, null, -1L, null), settings);
					made[2] += b == null ? 0 : b.getObjects().size();
				}
				nanos[2] = System.nanoTime() - start;
				start = System.nanoTime();
				for (RenderedObject d : drawn) {
					BaseDetailsObject b = searcher.searchDetailedObject(new AmenitySearcher.Request(d), settings, null);
					made[3] += b == null ? 0 : b.getObjects().size();
				}
				nanos[3] = System.nanoTime() - start;
				start = System.nanoTime();
				for (int i = 0; i < prefixed.size(); i++) {
					LatLon l = prefixed.get(i).getLocation();
					QuadRect r = MapUtils.calculateLatLonBbox(l.getLatitude(), l.getLongitude(), 5000);
					made[4] += searcher.searchAmenitiesByName(prefixes.get(i), r.top, r.left, r.bottom, r.right,
							l.getLatitude(), l.getLongitude(), null).size();
				}
				nanos[4] = System.nanoTime() - start;
				// merging changes the amenities it merges into: fresh ones, read before the clock starts
				List<List<Amenity>> fresh = new ArrayList<>();
				for (int f = 0; f < readers.size(); f++) {
					int[] b = boxes.get(f);
					fresh.add(readers.get(f).searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(b[0], b[1], b[2], b[3], -1, null, null, null)));
				}
				start = System.nanoTime();
				for (List<Amenity> list : fresh) {
					List<Amenity> merged = searcher.mergeAmenities(list, settings);
					made[5] += merged == null ? 0 : merged.size();
				}
				nanos[5] = System.nanoTime() - start;
				if (round >= WARMUP_ROUNDS) {
					for (int k = 0; k < KINDS.length; k++) {
						best[k] = Math.min(best[k], nanos[k] / 1e6);
						counts[k] = made[k];
					}
				}
			}
		} finally {
			for (BinaryMapIndexReader r : readers) {
				r.close();
			}
			MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		}
		System.out.println();
		System.out.println("### amenity searcher, java, " + readers.size() + " files, " + samples.size() + " amenities, best of " + MEASURED_ROUNDS);
		double total = 0;
		for (int k = 0; k < KINDS.length; k++) {
			System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", KINDS[k], best[k], counts[k]));
			total += best[k];
		}
		System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", "all", total, 0));
	}
}
