package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.util.KAlgorithms;
import net.osmand.shared.util.KMapAlgorithms;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link net.osmand.shared.map.OsmandRegions} and {@link net.osmand.shared.map.WorldRegion} against
 * the originals in OsmAnd-java, over the {@code regions.ocbf} that OsmAnd-java downloads into its
 * resources.
 *
 * Both sides read the file with the same locale and are then asked the same questions. The tree
 * is compared region by region, in its order, with every name, parameter, flag, centre, box and
 * polygon. The lookups by full name and by download name are compared for every region, and so are
 * the names built with their parents. Every object of the file is compared as the regions see it,
 * and so are the region queries at random points of the world and inside the regions, at the points
 * of the search tests and at the centre of every region. Last come the name matching and the
 * helpers that were copied along. Everything is compared in order.
 *
 * {@link #javaDumpIsWritten} also writes java's answers to {@code build/regions-java.txt}, which
 * {@code OsmandRegionsTest} in OsmAnd-shared holds the copy to on Kotlin/Native.
 */
public class OsmandRegionsCompatTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final int RANDOM_POINTS = 1000;
	private static final int REGION_POINTS = 3000;
	private static final String[][] LOCALES = {{"en", null}, {"de", null}, {"ru", null}, {"zh", "TW"}, {"zh", "CN"}};

	/** Read by {@code OsmandRegionsTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/regions-java.txt");
	private static final long FNV_OFFSET = 0xcbf29ce484222325L;
	private static final long FNV_PRIME = 0x100000001b3L;

	private static OsmandRegions java;
	private static net.osmand.shared.map.OsmandRegions copy;

	private static int regionsCompared;
	private static int objectsCompared;
	private static int pointsCompared;
	private static int namesCompared;
	private static int matchesCompared;

	@BeforeClass
	public static void open() throws IOException {
		assertTrue(REGIONS + " is missing; :OsmAnd-java:processResources downloads it", REGIONS.exists());
		java = new OsmandRegions(REGIONS.getPath());
		copy = new net.osmand.shared.map.OsmandRegions(REGIONS.getPath());
	}

	@AfterClass
	public static void close() throws IOException {
		java.close();
		copy.close();
		System.out.println("OsmandRegionsCompatTest: " + regionsCompared + " regions, " + objectsCompared
				+ " objects, " + pointsCompared + " points, " + namesCompared + " names and "
				+ matchesCompared + " name matches compared");
		assertTrue("regions compared: " + regionsCompared, regionsCompared > 10000);
		assertTrue("objects compared: " + objectsCompared, objectsCompared > 1000);
		assertTrue("points compared: " + pointsCompared, pointsCompared > RANDOM_POINTS + REGION_POINTS);
	}

	/** The tree as each locale builds it, and once with the continents translated. */
	@Test
	public void treesAreTheSame() throws IOException {
		compareTree("en", java.getWorldRegion(), copy.getWorldRegion());
		for (String[] locale : LOCALES) {
			compareTree(locale[0] + "_" + locale[1], prepared(locale, false));
		}
		compareTree("translated", prepared(LOCALES[0], true));
	}

	private void compareTree(String m, Object[] pair) throws IOException {
		OsmandRegions j = (OsmandRegions) pair[0];
		net.osmand.shared.map.OsmandRegions k = (net.osmand.shared.map.OsmandRegions) pair[1];
		try {
			compareTree(m, j.getWorldRegion(), k.getWorldRegion());
			assertEquals(m + " regions", ids(j.getAllRegionData()), ids(k.getAllRegionData()));
		} finally {
			j.close();
			k.close();
		}
	}

	private static Object[] prepared(String[] locale, boolean translate) throws IOException {
		OsmandRegions j = new OsmandRegions(false);
		net.osmand.shared.map.OsmandRegions k = new net.osmand.shared.map.OsmandRegions(false);
		j.setLocale(locale[0], locale[1]);
		k.setLocale(locale[0], locale[1]);
		if (translate) {
			j.setTranslator(id -> "T " + id.toUpperCase(Locale.US));
			k.setTranslator(id -> "T " + id.toUpperCase(Locale.US));
		}
		assertEquals("initialized", j.isInitialized(), k.isInitialized());
		j.prepareFile(REGIONS.getPath());
		k.prepareFile(REGIONS.getPath());
		assertEquals("initialized", j.isInitialized(), k.isInitialized());
		return new Object[] {j, k};
	}

	private void compareTree(String m, WorldRegion j, net.osmand.shared.map.WorldRegion k) {
		String n = m + " " + j.getRegionId();
		compareRegion(n, j, k);
		assertEquals(n + " subregions", ids(j.getSubregions()), ids(k.getSubregions()));
		for (int i = 0; i < j.getSubregions().size(); i++) {
			compareTree(m, j.getSubregions().get(i), k.getSubregions().get(i));
		}
	}

	/** Every region of the file, looked up by its full name and compared on its own. */
	@Test
	public void regionDataIsTheSame() {
		assertEquals("flattened", java.getFlattenedWorldRegionIds(), copy.getFlattenedWorldRegionIds());
		assertEquals("flattened first", java.getFlattenedWorldRegionIds().get(0), copy.getFlattenedWorldRegionIds().get(0));
		assertEquals("all", ids(java.getAllRegionData()), ids(copy.getAllRegionData()));
		for (WorldRegion j : java.getAllRegionData()) {
			compareRegion(j.getRegionId(), j, copy.getRegionData(j.getRegionId()));
		}
	}

	/** The lookups by name, and the names built with the parents. */
	@Test
	public void lookupsAreTheSame() {
		List<String> fullNames = new ArrayList<>(java.getFlattenedWorldRegionIds());
		fullNames.add("no_such_region");
		fullNames.add(null);
		for (String fullName : fullNames) {
			assertEquals(fullName, id(java.getRegionData(fullName)), id(copy.getRegionData(fullName)));
		}
		List<String> downloadNames = new ArrayList<>();
		for (WorldRegion r : java.getAllRegionData()) {
			if (r.getRegionDownloadName() != null) {
				downloadNames.add(r.getRegionDownloadName());
				downloadNames.add(r.getRegionDownloadName().toUpperCase(Locale.US));
			}
		}
		downloadNames.add("no_such_region");
		for (String d : downloadNames) {
			assertEquals(d, id(java.getRegionDataByDownloadName(d)), id(copy.getRegionDataByDownloadName(d)));
			assertEquals(d + " country", id(java.getCountryRegionDataByDownloadName(d)),
					id(copy.getCountryRegionDataByDownloadName(d)));
			WorldRegion jr = java.getRegionDataByDownloadName(d);
			List<WorldRegion> jbases = new ArrayList<>();
			jbases.add(null);
			if (jr != null) {
				jbases.add(jr.getCountryRegion());
				jbases.add(jr.getSuperregion());
			}
			for (boolean parent : new boolean[] {false, true}) {
				for (boolean reversed : new boolean[] {false, true}) {
					String n = d + " " + parent + " " + reversed;
					assertEquals(n, java.getLocaleName(d, parent, reversed), copy.getLocaleName(d, parent, reversed));
					assertEquals(n, java.getLocaleName(d, parent), copy.getLocaleName(d, parent));
					for (String divider : new String[] {" ", ", ", " / "}) {
						assertEquals(n + " '" + divider + "'", java.getLocaleName(d, divider, parent, reversed),
								copy.getLocaleName(d, divider, parent, reversed));
					}
					for (WorldRegion jbase : jbases) {
						net.osmand.shared.map.WorldRegion kbase = jbase == null ? null
								: copy.getRegionData(jbase.getRegionId());
						assertEquals(n + " " + jbase, java.getLocaleName(d, parent, jbase, reversed),
								copy.getLocaleName(d, parent, kbase, reversed));
						assertEquals(n + " " + jbase, java.getLocaleName(d, " / ", parent, jbase, reversed),
								copy.getLocaleName(d, " / ", parent, kbase, reversed));
					}
					namesCompared++;
				}
			}
		}
		for (String fullName : fullNames) {
			if (fullName == null) {
				continue;
			}
			for (boolean parent : new boolean[] {false, true}) {
				for (boolean reversed : new boolean[] {false, true}) {
					assertEquals(fullName, java.getLocaleNameByFullName(fullName, ", ", parent, reversed),
							copy.getLocaleNameByFullName(fullName, ", ", parent, reversed));
					namesCompared++;
				}
			}
		}
	}

	/** Every object of the file as the regions see it: its names, its download types, its area, its shape. */
	@Test
	public void objectsAreTheSame() throws IOException {
		List<BinaryMapDataObject> jobjects = javaObjects(java.getReader());
		List<net.osmand.shared.binary.BinaryMapDataObject> kobjects = copyObjects(copy.getReader());
		assertEquals("objects", jobjects.size(), kobjects.size());
		Random random = new Random(24092026L);
		String[] types = {OsmandRegions.MAP_TYPE, OsmandRegions.ROADS_TYPE, OsmandRegions.MAP_JOIN_TYPE,
				OsmandRegions.ROADS_JOIN_TYPE, "no_such_type"};
		for (int i = 0; i < jobjects.size(); i++) {
			BinaryMapDataObject j = jobjects.get(i);
			net.osmand.shared.binary.BinaryMapDataObject k = kobjects.get(i);
			String m = "object " + i + " " + j.getId();
			assertEquals(m + " id", j.getId(), k.getId());
			assertEquals(m + " download name", java.getDownloadName(j), copy.getDownloadName(k));
			assertEquals(m + " full name", java.getFullName(j), copy.getFullName(k));
			for (String type : types) {
				assertEquals(m + " " + type, java.isDownloadOfType(j, type), copy.isDownloadOfType(k, type));
			}
			assertEquals(m + " area", OsmandRegions.getArea(j), net.osmand.shared.map.OsmandRegions.getArea(k), 0);
			if (j.getPointsLength() == 0) {
				objectsCompared++;
				continue;
			}
			for (int p = 0; p < 20; p++) {
				int x;
				int y;
				if (p < 5) {
					int n = random.nextInt(j.getPointsLength());
					x = j.getPoint31XTile(n) + random.nextInt(2001) - 1000;
					y = j.getPoint31YTile(n) + random.nextInt(2001) - 1000;
				} else {
					x = randomX(random);
					y = randomY(random);
				}
				assertEquals(m + " contain " + x + " " + y, OsmandRegions.contain(j, x, y),
						net.osmand.shared.map.OsmandRegions.contain(k, x, y));
				int size = 1 << (10 + random.nextInt(18));
				int lx = x - size / 2;
				int ty = y - size / 2;
				int rx = lx + size;
				int by = ty + size;
				assertEquals(m + " intersect " + lx + " " + ty + " " + rx + " " + by,
						OsmandRegions.intersect(j, lx, ty, rx, by),
						net.osmand.shared.map.OsmandRegions.intersect(k, lx, ty, rx, by));
			}
			objectsCompared++;
		}
	}

	/**
	 * What the regions say about a point: random ones over the world, random ones inside the box of a
	 * random region, where the borders are, the points of the search tests and the centre of every
	 * region.
	 */
	@Test
	public void pointsAreTheSame() throws IOException {
		List<LatLon> points = new ArrayList<>();
		Random random = new Random(24092026L);
		for (int i = 0; i < RANDOM_POINTS; i++) {
			points.add(new LatLon(random.nextDouble() * 170 - 85, random.nextDouble() * 360 - 180));
		}
		List<WorldRegion> regions = java.getAllRegionData();
		while (points.size() < RANDOM_POINTS + REGION_POINTS) {
			QuadRect box = regions.get(random.nextInt(regions.size())).getBoundingBox();
			if (box != null) {
				double lat = Math.min(box.top, box.bottom) + random.nextDouble() * Math.abs(box.top - box.bottom);
				double lon = box.left + random.nextDouble() * (box.right - box.left);
				points.add(new LatLon(lat, lon));
			}
		}
		points.addAll(searchTestPoints());
		for (WorldRegion r : regions) {
			if (r.getRegionCenter() != null) {
				points.add(r.getRegionCenter());
			}
		}
		for (int i = 0; i < points.size(); i++) {
			comparePoint(i, points.get(i), random);
		}
	}

	/** Each point asks every question once, the overloads that only pass a default on every tenth. */
	private void comparePoint(int i, LatLon ll, Random random) throws IOException {
		String m = ll.getLatitude() + " " + ll.getLongitude();
		KLatLon kll = new KLatLon(ll.getLatitude(), ll.getLongitude());
		int x = MapUtils.get31TileNumberX(ll.getLongitude());
		int y = MapUtils.get31TileNumberY(ll.getLatitude());
		List<BinaryMapDataObject> jquery = java.query(x, y);
		List<net.osmand.shared.binary.BinaryMapDataObject> kquery = copy.query(x, y);
		assertEquals(m + " query", describe(jquery), describeCopy(kquery));
		assertEquals(m + " filtered", describe(java.filterQueryResultsByPoint(jquery, x, y)),
				describeCopy(copy.filterQueryResultsByPoint(kquery, x, y)));
		int size = 1 << (12 + random.nextInt(16));
		boolean checkCenter = i % 2 == 0;
		assertEquals(m + " query box " + checkCenter,
				describe(java.query(x - size, x + size, y - size, y + size, checkCenter)),
				describeCopy(copy.query(x - size, x + size, y - size, y + size, checkCenter)));
		assertEquals(m + " country", java.getCountryName(ll), copy.getCountryName(kll));
		assertEquals(m + " download", describe(java.getRegionsToDownload(ll.getLatitude(), ll.getLongitude())),
				describeCopy(copy.getRegionsToDownload(ll.getLatitude(), ll.getLongitude())));
		assertEquals(m + " download names",
				java.getRegionsToDownload(ll.getLatitude(), ll.getLongitude(), new ArrayList<>(Arrays.asList("stale"))),
				copy.getRegionsToDownload(ll.getLatitude(), ll.getLongitude(), new ArrayList<>(Arrays.asList("stale"))));
		boolean roads = i % 2 == 1;
		List<WorldRegion> jat = java.getWorldRegionsAt(ll, roads);
		assertEquals(m + " regions at " + roads, ids(jat), ids(copy.getWorldRegionsAt(kll, roads)));
		for (WorldRegion r : jat) {
			assertEquals(m + " contains " + r.getRegionId(), r.containsPoint(ll),
					copy.getRegionData(r.getRegionId()).containsPoint(kll));
		}
		Map.Entry<WorldRegion, BinaryMapDataObject> j = java.getSmallestBinaryMapDataObjectAt(ll);
		Map.Entry<net.osmand.shared.map.WorldRegion, net.osmand.shared.binary.BinaryMapDataObject> k =
				copy.getSmallestBinaryMapDataObjectAt(kll);
		assertEquals(m + " smallest", j == null ? null : j.getKey().getRegionId() + " " + j.getValue().getId(),
				k == null ? null : k.getKey().getRegionId() + " " + k.getValue().getId());
		if (i % 10 == 0) {
			assertEquals(m + " query box", describe(java.query(x - size, x + size, y - size, y + size)),
					describeCopy(copy.query(x - size, x + size, y - size, y + size)));
			assertEquals(m + " regions at", ids(java.getWorldRegionsAt(ll)), ids(copy.getWorldRegionsAt(kll)));
		}
		pointsCompared++;
	}

	/** The name matching of the region search, on the words of each region and of the next one. */
	@Test
	public void namesMatchTheSame() {
		List<WorldRegion> regions = java.getAllRegionData();
		String[] fixed = {"1", "PA", "pa", "55", "5", "7", "4", "22", "33", "44", "", "-", "united", "UNITED"};
		for (int i = 0; i < regions.size(); i++) {
			String text = regions.get(i).getRegionSearchText();
			String next = regions.get((i + 1) % regions.size()).getRegionSearchText();
			Set<String> queries = new LinkedHashSet<>(Arrays.asList(fixed));
			for (String t : new String[] {text, next}) {
				if (t == null) {
					continue;
				}
				String[] words = t.trim().split(" ");
				for (int w = 0; w < Math.min(4, words.length); w++) {
					queries.add(words[w]);
					queries.add(words[w].toUpperCase(Locale.US));
					queries.add(words[w].substring(0, Math.min(2, words[w].length())));
					if (w > 0) {
						queries.add(words[w - 1] + " " + words[w]);
					}
				}
			}
			for (String q : queries) {
				for (String name : new String[] {text, null, ""}) {
					assertEquals("'" + q + "' in " + name, OsmandRegions.isRegionNameMatched(q, name),
							net.osmand.shared.map.OsmandRegions.isRegionNameMatched(q, name));
					matchesCompared++;
				}
			}
		}
	}

	/** Containment between regions of one parent, and what removing the duplicates among them leaves. */
	@Test
	public void containmentIsTheSame() {
		List<WorldRegion> parents = new ArrayList<>(java.getAllRegionData());
		parents.add(java.getWorldRegion());
		for (WorldRegion parent : parents) {
			List<WorldRegion> j = parent.getSubregions();
			List<net.osmand.shared.map.WorldRegion> k = new ArrayList<>();
			for (WorldRegion r : j) {
				k.add(copy.getRegionData(r.getRegionId()));
			}
			for (int a = 0; a < j.size(); a++) {
				for (int b = 0; b < j.size(); b++) {
					assertEquals(j.get(a) + " contains " + j.get(b), j.get(a).containsRegion(j.get(b)),
							k.get(a).containsRegion(k.get(b)));
				}
			}
			assertEquals(parent + " without duplicates", ids(WorldRegion.removeDuplicates(j)),
					ids(net.osmand.shared.map.WorldRegion.removeDuplicates(k)));
		}
	}

	/** The file names a download name makes, and back. */
	@Test
	public void fileNamesAreTheSame() {
		List<String> names = new ArrayList<>();
		for (WorldRegion r : java.getAllRegionData()) {
			if (r.getRegionDownloadName() != null) {
				names.add(r.getRegionDownloadName());
			}
		}
		names.addAll(Arrays.asList("", "a", "A", "Europe_GB", "x.road", null));
		for (String n : names) {
			assertEquals(n, WorldRegion.getObfFileName(n), net.osmand.shared.map.WorldRegion.getObfFileName(n));
			assertEquals(n, WorldRegion.getRoadObfFileName(n), net.osmand.shared.map.WorldRegion.getRoadObfFileName(n));
			if (n == null) {
				continue;
			}
			for (String f : new String[] {n, WorldRegion.getObfFileName(n), WorldRegion.getRoadObfFileName(n),
					n.toUpperCase(Locale.US) + ".obf", n + ".OBF"}) {
				assertEquals(f, WorldRegion.getRegionDownloadName(f), net.osmand.shared.map.WorldRegion.getRegionDownloadName(f));
			}
		}
	}

	/** The helpers of {@code Algorithms} and {@code MapAlgorithms} that the regions brought along. */
	@Test
	public void helpersAreTheSame() {
		Random random = new Random(24092026L);
		String[] strings = {"", "-", "1", "-1", "12", "-12", "1-2", "--1", "a", "1a", "٣", "١٢", "+1", " 1", "1 ",
				"Europe", "EUROPE", "é", "ÉCOLE", "straße", "İstanbul", "ǅ", "x"};
		for (String s : strings) {
			assertEquals("isInt " + s, Algorithms.isInt(s), KAlgorithms.INSTANCE.isInt(s));
			assertEquals("capitalize " + s, Algorithms.capitalizeFirstLetterAndLowercase(s),
					KAlgorithms.INSTANCE.capitalizeFirstLetterAndLowercase(s));
		}
		assertEquals(Algorithms.capitalizeFirstLetterAndLowercase(null), KAlgorithms.INSTANCE.capitalizeFirstLetterAndLowercase(null));
		for (int i = 0; i < 200000; i++) {
			int size = 1 << (4 + random.nextInt(24));
			int lx = randomX(random);
			int ty = randomY(random);
			int rx = lx + size;
			int by = ty + size;
			int inx = lx + random.nextInt(size + 1);
			int iny = ty + random.nextInt(size + 1);
			int outx = random.nextBoolean() ? lx - random.nextInt(size) : rx + random.nextInt(size);
			int outy = random.nextInt(3) == 0 ? (random.nextBoolean() ? ty : by) : ty - size + random.nextInt(3 * size);
			if (random.nextInt(10) == 0) {
				int t = inx;
				inx = outx;
				outx = t;
			}
			assertEquals(inx + " " + iny + " " + outx + " " + outy,
					MapAlgorithms.calculateIntersection(inx, iny, outx, outy, lx, rx, by, ty),
					KMapAlgorithms.INSTANCE.calculateIntersection(inx, iny, outx, outy, lx, rx, by, ty));
		}
		for (WorldRegion r : java.getAllRegionData()) {
			List<float[]> polygons = r.getPolygons();
			for (int a = 0; a < polygons.size(); a++) {
				for (int b = 0; b < polygons.size(); b++) {
					assertEquals(r + " polygon " + a + " in " + b,
							Algorithms.isFirstPolygonInsideSecond(polygons.get(a), polygons.get(b)),
							KAlgorithms.INSTANCE.isFirstPolygonInsideSecond(polygons.get(a), polygons.get(b)));
				}
				for (int p = 0; p < 10; p++) {
					float lat = (float) (random.nextDouble() * 170 - 85);
					float lon = (float) (random.nextDouble() * 360 - 180);
					if (p < 5 && polygons.get(a).length > 0) {
						int n = random.nextInt(polygons.get(a).length / 2) * 2;
						lat = polygons.get(a)[n] + (float) random.nextGaussian() * 0.01f;
						lon = polygons.get(a)[n + 1] + (float) random.nextGaussian() * 0.01f;
					}
					assertEquals(r + " point " + lat + " " + lon,
							Algorithms.isPointInsidePolygon(lat, lon, polygons.get(a)),
							KAlgorithms.INSTANCE.isPointInsidePolygon(lat, lon, polygons.get(a)));
				}
			}
		}
	}

	/**
	 * Java's answers, written to {@link #JAVA_DUMP} for {@code OsmandRegionsTest} in OsmAnd-shared,
	 * which builds the same text with the copy wherever it runs and compares the two. That is how
	 * Kotlin/Native is held to java: its collator, its lowercasing and its string splitting are its
	 * own. The copy on the jvm has to write the same text here first.
	 */
	@Test
	public void javaDumpIsWritten() throws IOException {
		String dump = javaDump();
		assertEquals("dump", dump, copyDump());
		JAVA_DUMP.getParentFile().mkdirs();
		Files.write(JAVA_DUMP.toPath(), dump.getBytes(StandardCharsets.UTF_8));
	}

	private static String javaDump() throws IOException {
		StringBuilder sb = new StringBuilder();
		List<WorldRegion> tree = new ArrayList<>();
		flatten(java.getWorldRegion(), tree);
		for (WorldRegion r : tree) {
			String center = r.getRegionCenter() == null ? "~"
					: bits(r.getRegionCenter().getLatitude()) + "," + bits(r.getRegionCenter().getLongitude());
			QuadRect b = r.getBoundingBox();
			String box = b == null ? "~" : bits(b.left) + "," + bits(b.top) + "," + bits(b.right) + "," + bits(b.bottom);
			long hash = FNV_OFFSET;
			for (float[] polygon : r.getPolygons()) {
				hash = (hash ^ polygon.length) * FNV_PRIME;
				for (float f : polygon) {
					hash = (hash ^ (Float.floatToRawIntBits(f) & 0xffffffffL)) * FNV_PRIME;
				}
			}
			WorldRegion.RegionParams p = r.getParams();
			sb.append("R ").append(r.getRegionId()).append('|').append(or(r.getRegionDownloadName()))
					.append('|').append(r.getLocaleName()).append('|').append(or(field(r, "regionName")))
					.append('|').append(or(field(r, "regionNameEn"))).append('|').append(or(field(r, "regionNameLocale")))
					.append('|').append(or(field(r, "regionParentFullName"))).append('|').append(or(r.getRegionSearchText()))
					.append('|').append(flag(r.isRegionMapDownload())).append(flag(r.isRegionRoadsDownload()))
					.append(flag(r.isRegionJoinMapDownload())).append(flag(r.isRegionJoinRoadsDownload()))
					.append('|').append(or(p.getRegionLang())).append('|').append(or(p.getRegionLeftHandDriving()))
					.append('|').append(or(p.getRegionMetric())).append('|').append(or(p.getRegionRoadSigns()))
					.append('|').append(or(p.getWikiLink())).append('|').append(or(p.getPopulation()))
					.append('|').append(center).append('|').append(box).append('|').append(Long.toHexString(hash))
					.append('|').append(r.getLevel()).append('|').append(flag(r.isContinent()))
					.append('|').append(or(id(r.getCountryRegion()))).append('|').append(r.getSubregions().size())
					.append('\n');
		}
		for (LatLon ll : dumpPoints(tree)) {
			Map.Entry<WorldRegion, BinaryMapDataObject> smallest = java.getSmallestBinaryMapDataObjectAt(ll);
			List<String> names = java.getRegionsToDownload(ll.getLatitude(), ll.getLongitude(), new ArrayList<>());
			sb.append("P ").append(bits(ll.getLatitude())).append(',').append(bits(ll.getLongitude()))
					.append('|').append(smallest == null ? "~" : smallest.getKey().getRegionId())
					.append('|').append(String.join(",", names))
					.append('|').append(or(java.getCountryName(ll))).append('\n');
		}
		return sb.toString();
	}

	private static String copyDump() throws IOException {
		StringBuilder sb = new StringBuilder();
		List<net.osmand.shared.map.WorldRegion> tree = new ArrayList<>();
		flatten(copy.getWorldRegion(), tree);
		for (net.osmand.shared.map.WorldRegion r : tree) {
			String center = r.getRegionCenter() == null ? "~"
					: bits(r.getRegionCenter().getLatitude()) + "," + bits(r.getRegionCenter().getLongitude());
			KQuadRect b = r.getBoundingBox();
			String box = b == null ? "~"
					: bits(b.getLeft()) + "," + bits(b.getTop()) + "," + bits(b.getRight()) + "," + bits(b.getBottom());
			long hash = FNV_OFFSET;
			for (float[] polygon : r.getPolygons()) {
				hash = (hash ^ polygon.length) * FNV_PRIME;
				for (float f : polygon) {
					hash = (hash ^ (Float.floatToRawIntBits(f) & 0xffffffffL)) * FNV_PRIME;
				}
			}
			net.osmand.shared.map.WorldRegion.RegionParams p = r.getParams();
			sb.append("R ").append(r.getRegionId()).append('|').append(or(r.getRegionDownloadName()))
					.append('|').append(r.getLocaleName()).append('|').append(or(r.regionName))
					.append('|').append(or(r.regionNameEn)).append('|').append(or(r.regionNameLocale))
					.append('|').append(or(r.regionParentFullName)).append('|').append(or(r.getRegionSearchText()))
					.append('|').append(flag(r.isRegionMapDownload())).append(flag(r.isRegionRoadsDownload()))
					.append(flag(r.isRegionJoinMapDownload())).append(flag(r.isRegionJoinRoadsDownload()))
					.append('|').append(or(p.getRegionLang())).append('|').append(or(p.getRegionLeftHandDriving()))
					.append('|').append(or(p.getRegionMetric())).append('|').append(or(p.getRegionRoadSigns()))
					.append('|').append(or(p.getWikiLink())).append('|').append(or(p.getPopulation()))
					.append('|').append(center).append('|').append(box).append('|').append(Long.toHexString(hash))
					.append('|').append(r.getLevel()).append('|').append(flag(r.isContinent()))
					.append('|').append(or(id(r.getCountryRegion()))).append('|').append(r.getSubregions().size())
					.append('\n');
		}
		List<LatLon> points = new ArrayList<>();
		for (net.osmand.shared.map.WorldRegion r : tree) {
			if (r.getRegionCenter() != null) {
				points.add(new LatLon(r.getRegionCenter().getLatitude(), r.getRegionCenter().getLongitude()));
			}
		}
		for (LatLon ll : dumpPoints(points)) {
			KLatLon kll = new KLatLon(ll.getLatitude(), ll.getLongitude());
			Map.Entry<net.osmand.shared.map.WorldRegion, net.osmand.shared.binary.BinaryMapDataObject> smallest =
					copy.getSmallestBinaryMapDataObjectAt(kll);
			List<String> names = copy.getRegionsToDownload(ll.getLatitude(), ll.getLongitude(), new ArrayList<>());
			sb.append("P ").append(bits(ll.getLatitude())).append(',').append(bits(ll.getLongitude()))
					.append('|').append(smallest == null ? "~" : smallest.getKey().getRegionId())
					.append('|').append(String.join(",", names))
					.append('|').append(or(copy.getCountryName(kll))).append('\n');
		}
		return sb.toString();
	}

	/** A grid over the world every 6 degrees, then the centre of every region of the tree, in its order. */
	private static List<LatLon> dumpPoints(List<?> treeOrCenters) {
		List<LatLon> points = new ArrayList<>();
		for (int lat = -78; lat <= 78; lat += 6) {
			for (int lon = -180; lon < 180; lon += 6) {
				points.add(new LatLon(lat, lon));
			}
		}
		for (Object o : treeOrCenters) {
			if (o instanceof LatLon) {
				points.add((LatLon) o);
			} else if (((WorldRegion) o).getRegionCenter() != null) {
				points.add(((WorldRegion) o).getRegionCenter());
			}
		}
		return points;
	}

	private static void flatten(WorldRegion r, List<WorldRegion> result) {
		result.add(r);
		for (WorldRegion s : r.getSubregions()) {
			flatten(s, result);
		}
	}

	private static void flatten(net.osmand.shared.map.WorldRegion r, List<net.osmand.shared.map.WorldRegion> result) {
		result.add(r);
		for (net.osmand.shared.map.WorldRegion s : r.getSubregions()) {
			flatten(s, result);
		}
	}

	private static String bits(double d) {
		return Long.toHexString(Double.doubleToRawLongBits(d));
	}

	private static String or(Object s) {
		return s == null ? "~" : s.toString();
	}

	private static String flag(boolean b) {
		return b ? "1" : "0";
	}

	private void compareRegion(String m, WorldRegion j, net.osmand.shared.map.WorldRegion k) {
		assertEquals(m + " present", j != null, k != null);
		if (j == null) {
			return;
		}
		assertEquals(m + " id", j.getRegionId(), k.getRegionId());
		assertEquals(m + " toString", j.toString(), k.toString());
		assertEquals(m + " download name", j.getRegionDownloadName(), k.getRegionDownloadName());
		assertEquals(m + " download name lc", j.getRegionDownloadNameLC(), k.getRegionDownloadNameLC());
		assertEquals(m + " locale name", j.getLocaleName(), k.getLocaleName());
		assertEquals(m + " name", field(j, "regionName"), k.regionName);
		assertEquals(m + " name en", field(j, "regionNameEn"), k.regionNameEn);
		assertEquals(m + " name locale", field(j, "regionNameLocale"), k.regionNameLocale);
		assertEquals(m + " parent name", field(j, "regionParentFullName"), k.regionParentFullName);
		assertEquals(m + " search text", j.getRegionSearchText(), k.getRegionSearchText());
		assertEquals(m + " map", j.isRegionMapDownload(), k.isRegionMapDownload());
		assertEquals(m + " roads", j.isRegionRoadsDownload(), k.isRegionRoadsDownload());
		assertEquals(m + " join map", j.isRegionJoinMapDownload(), k.isRegionJoinMapDownload());
		assertEquals(m + " join roads", j.isRegionJoinRoadsDownload(), k.isRegionJoinRoadsDownload());
		assertEquals(m + " lang", j.getParams().getRegionLang(), k.getParams().getRegionLang());
		assertEquals(m + " left hand", j.getParams().getRegionLeftHandDriving(), k.getParams().getRegionLeftHandDriving());
		assertEquals(m + " metric", j.getParams().getRegionMetric(), k.getParams().getRegionMetric());
		assertEquals(m + " road signs", j.getParams().getRegionRoadSigns(), k.getParams().getRegionRoadSigns());
		assertEquals(m + " wiki", j.getParams().getWikiLink(), k.getParams().getWikiLink());
		assertEquals(m + " population", j.getParams().getPopulation(), k.getParams().getPopulation());
		assertEquals(m + " center", describe(j.getRegionCenter()), describe(k.getRegionCenter()));
		assertEquals(m + " box", describe(j.getBoundingBox()), describe(k.getBoundingBox()));
		assertEquals(m + " boundaries", j.hasBoundaries(), k.hasBoundaries());
		List<float[]> jpolygons = j.getPolygons();
		List<float[]> kpolygons = k.getPolygons();
		assertEquals(m + " polygons", jpolygons.size(), kpolygons.size());
		for (int i = 0; i < jpolygons.size(); i++) {
			assertArrayEquals(m + " polygon " + i, jpolygons.get(i), kpolygons.get(i), 0);
		}
		List<QuadRect> jbounds = j.getAllPolygonsBounds();
		List<KQuadRect> kbounds = k.getAllPolygonsBounds();
		assertEquals(m + " bounds", jbounds.size(), kbounds.size());
		for (int i = 0; i < jbounds.size(); i++) {
			assertEquals(m + " bounds " + i, describe(jbounds.get(i)), describe(kbounds.get(i)));
		}
		assertEquals(m + " level", j.getLevel(), k.getLevel());
		assertEquals(m + " continent", j.isContinent(), k.isContinent());
		assertEquals(m + " country", id(j.getCountryRegion()), id(k.getCountryRegion()));
		assertEquals(m + " superregion", id(j.getSuperregion()), id(k.getSuperregion()));
		assertEquals(m + " superregions", ids(j.getSuperRegions()), ids(k.getSuperRegions()));
		assertEquals(m + " obf", j.getObfFileName(), k.getObfFileName());
		assertEquals(m + " road obf", j.getRoadObfFileName(), k.getRoadObfFileName());
		regionsCompared++;
	}

	private static Object field(WorldRegion region, String name) {
		try {
			Field f = WorldRegion.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(region);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<BinaryMapDataObject> javaObjects(BinaryMapIndexReader reader) throws IOException {
		List<BinaryMapDataObject> result = new ArrayList<>();
		SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(0, Integer.MAX_VALUE, 0,
				Integer.MAX_VALUE, 5, (types, index) -> true, new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject object) {
						result.add(object);
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
		reader.searchMapIndex(sr);
		return result;
	}

	private static List<net.osmand.shared.binary.BinaryMapDataObject> copyObjects(
			net.osmand.shared.binary.BinaryMapIndexReader reader) {
		List<net.osmand.shared.binary.BinaryMapDataObject> result = new ArrayList<>();
		net.osmand.shared.binary.SearchRequest<net.osmand.shared.binary.BinaryMapDataObject> sr =
				net.osmand.shared.binary.SearchRequest.buildSearchRequest(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 5,
						(types, index) -> true,
						new net.osmand.shared.binary.ResultMatcher<net.osmand.shared.binary.BinaryMapDataObject>() {
							@Override
							public boolean publish(net.osmand.shared.binary.BinaryMapDataObject object) {
								result.add(object);
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
		reader.searchMapIndex(sr);
		return result;
	}

	/** The points the search test cases search around. */
	private static List<LatLon> searchTestPoints() throws IOException {
		List<LatLon> points = new ArrayList<>();
		File[] cases = new File("src/test/resources/search").listFiles((dir, name) -> name.endsWith(".json"));
		assertTrue("search test cases", cases != null && cases.length > 0);
		Arrays.sort(cases);
		Pattern lat = Pattern.compile("\"lat\"\\s*:\\s*\"?([-0-9.]+)");
		Pattern lon = Pattern.compile("\"lon\"\\s*:\\s*\"?([-0-9.]+)");
		for (File f : cases) {
			String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
			Matcher la = lat.matcher(text);
			Matcher lo = lon.matcher(text);
			if (la.find() && lo.find()) {
				points.add(new LatLon(Double.parseDouble(la.group(1)), Double.parseDouble(lo.group(1))));
			}
		}
		return points;
	}

	private static int randomX(Random random) {
		return random.nextInt(Integer.MAX_VALUE);
	}

	private static int randomY(Random random) {
		return MapUtils.get31TileNumberY(random.nextDouble() * 170 - 85);
	}

	private static String describe(List<BinaryMapDataObject> objects) {
		StringBuilder sb = new StringBuilder();
		for (BinaryMapDataObject o : objects) {
			sb.append(o.getId()).append(':').append(java.getFullName(o)).append(':').append(o.getPointsLength()).append(' ');
		}
		return sb.toString();
	}

	private static String describeCopy(List<net.osmand.shared.binary.BinaryMapDataObject> objects) {
		StringBuilder sb = new StringBuilder();
		for (net.osmand.shared.binary.BinaryMapDataObject o : objects) {
			sb.append(o.getId()).append(':').append(copy.getFullName(o)).append(':').append(o.getPointsLength()).append(' ');
		}
		return sb.toString();
	}

	private static String describe(LatLon l) {
		return l == null ? null : l.getLatitude() + " " + l.getLongitude();
	}

	private static String describe(KLatLon l) {
		return l == null ? null : l.getLatitude() + " " + l.getLongitude();
	}

	private static String describe(QuadRect r) {
		return r == null ? null : r.left + " " + r.top + " " + r.right + " " + r.bottom;
	}

	private static String describe(KQuadRect r) {
		return r == null ? null : r.getLeft() + " " + r.getTop() + " " + r.getRight() + " " + r.getBottom();
	}

	private static String id(WorldRegion r) {
		return r == null ? null : r.getRegionId();
	}

	private static String id(net.osmand.shared.map.WorldRegion r) {
		return r == null ? null : r.getRegionId();
	}

	private static List<String> ids(List<? extends WorldRegion> regions) {
		List<String> result = new ArrayList<>();
		for (WorldRegion r : regions) {
			result.add(r.getRegionId());
		}
		return result;
	}

	private static List<String> ids(Iterable<net.osmand.shared.map.WorldRegion> regions) {
		List<String> result = new ArrayList<>();
		for (net.osmand.shared.map.WorldRegion r : regions) {
			result.add(r.getRegionId());
		}
		return result;
	}
}
