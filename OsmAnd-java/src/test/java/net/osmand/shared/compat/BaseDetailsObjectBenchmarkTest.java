package net.osmand.shared.compat;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The java half of {@code BaseDetailsObjectBenchmarkTest} in OsmAnd-shared: how long it takes to make
 * details objects of the objects {@link BaseDetailsObjectCompatTest} wrote to its dump - single
 * amenities, amenities with one osm id or wikidata as the search unites them, and drawn objects with
 * their amenity.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_DETAILS_OBJECTS_BENCHMARK}
 * is set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_DETAILS_OBJECTS_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*BaseDetailsObjectBenchmarkTest" --rerun -i
 * </pre>
 */
public class BaseDetailsObjectBenchmarkTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/details-objects-java.txt");
	private static final File MAPS = new File("build/search-obf");
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 5;
	private static final String[] KINDS = {"single amenities", "united amenities", "drawn objects"};

	@Test
	public void benchmarkDetailsObjects() throws Exception {
		Assume.assumeTrue("set OSMAND_DETAILS_OBJECTS_BENCHMARK to run", System.getenv("OSMAND_DETAILS_OBJECTS_BENCHMARK") != null);
		Assume.assumeTrue("run BaseDetailsObjectCompatTest first", DUMP.exists());
		Map<String, String> phrases = SearchApisCompatTest.poiPhrases;
		List<Amenity> singles = new ArrayList<>();
		List<List<Amenity>> groups = new ArrayList<>();
		List<Amenity> drawnAmenities = new ArrayList<>();
		List<RenderedObject> drawn = new ArrayList<>();
		Map<String, List<Amenity>> amenities = new HashMap<>();
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		try (BufferedReader in = Files.newBufferedReader(DUMP.toPath(), StandardCharsets.UTF_8)) {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				String[] f = line.split("\t");
				switch (f[0]) {
					case "X" -> phrases.put(SearchPhraseCompatTest.unhex(f[1]), SearchPhraseCompatTest.unhex(f[2]));
					case "S" -> singles.add(amenities(amenities, f[1]).get(Integer.parseInt(f[2])));
					case "U" -> {
						List<Amenity> group = new ArrayList<>();
						for (String m : f[1].split(",")) {
							String[] fi = m.split(":");
							group.add(amenities(amenities, fi[0]).get(Integer.parseInt(fi[1])));
						}
						groups.add(group);
					}
					case "R" -> {
						drawnAmenities.add(amenities(amenities, f[1]).get(Integer.parseInt(f[2])));
						drawn.add(drawn(f[3]));
					}
					default -> {
					}
				}
			}
		}
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator());

		double[] best = new double[KINDS.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		long made = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] nanos = new long[KINDS.length];
			made = 0;
			long start = System.nanoTime();
			for (Amenity a : singles) {
				made += new BaseDetailsObject(a, "en").getObjects().size();
			}
			nanos[0] = System.nanoTime() - start;
			start = System.nanoTime();
			for (List<Amenity> group : groups) {
				BaseDetailsObject b = new BaseDetailsObject(group.get(0), "en");
				for (int i = 1; i < group.size(); i++) {
					b.addObject(group.get(i));
				}
				made += b.getObjects().size();
			}
			nanos[1] = System.nanoTime() - start;
			start = System.nanoTime();
			for (int i = 0; i < drawn.size(); i++) {
				RenderedObject d = drawn.get(i);
				BaseDetailsObject b = new BaseDetailsObject(drawnAmenities.get(i), "en");
				b.addObject(d);
				made += b.getObjects().size() + BaseDetailsObject.convertRenderedObjectToAmenity(d, MapPoiTypes.getDefault())
						.getAdditionalInfoKeys().size();
			}
			nanos[2] = System.nanoTime() - start;
			if (round >= WARMUP_ROUNDS) {
				for (int k = 0; k < KINDS.length; k++) {
					best[k] = Math.min(best[k], nanos[k] / 1e6);
				}
			}
		}
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		System.out.println();
		System.out.println("### details objects, java, " + singles.size() + " single, " + groups.size() + " united, "
				+ drawn.size() + " drawn, best of " + MEASURED_ROUNDS + " (" + made + " objects)");
		double total = 0;
		for (int k = 0; k < KINDS.length; k++) {
			System.out.println(String.format(java.util.Locale.US, "  %-24s %9.1f", KINDS[k], best[k]));
			total += best[k];
		}
		System.out.println(String.format(java.util.Locale.US, "  %-24s %9.1f", "all", total));
	}

	private static List<Amenity> amenities(Map<String, List<Amenity>> amenities, String hexName) throws Exception {
		List<Amenity> list = amenities.get(hexName);
		if (list == null) {
			File f = new File(MAPS, SearchPhraseCompatTest.unhex(hexName));
			BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			try {
				list = r.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null, null));
			} finally {
				r.close();
			}
			amenities.put(hexName, list);
		}
		return list;
	}

	/** The drawn object {@link BaseDetailsObjectCompatTest#spec} wrote. */
	static RenderedObject drawn(String spec) {
		String[] f = spec.split(",", -1);
		RenderedObject d = new RenderedObject();
		if (!f[3].isEmpty()) {
			for (String tag : f[3].split(";")) {
				String[] kv = tag.split("=", -1);
				d.putTag(SearchPhraseCompatTest.unhex(kv[0]), SearchPhraseCompatTest.unhex(kv[1]));
			}
		}
		d.setName(SearchPhraseCompatTest.unhex(f[1]));
		d.setId("-".equals(f[0]) ? null : Long.parseLong(f[0]));
		if (!"-".equals(f[2])) {
			d.setLocation(latLon(f[2]));
		}
		if (!f[4].isEmpty()) {
			String[] xy = f[4].split(" ");
			for (int p = 0; p + 1 < xy.length; p += 2) {
				d.addLocation(Integer.parseInt(xy[p]), Integer.parseInt(xy[p + 1]));
			}
		}
		d.setLabelX(Integer.parseInt(f[5]));
		d.setLabelY(Integer.parseInt(f[6]));
		if (!"-".equals(f[7])) {
			d.setLabelLatLon(latLon(f[7]));
		}
		return d;
	}

	private static LatLon latLon(String bits) {
		String[] ll = bits.split(" ");
		return new LatLon(SearchPhraseCompatTest.unbits(ll[0]), SearchPhraseCompatTest.unbits(ll[1]));
	}
}
