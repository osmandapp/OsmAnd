package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.call;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.Location;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.AmenitySearcher;
import net.osmand.search.core.AmenityIndexRepository;
import net.osmand.util.MapUtils;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * {@code AmenitySearcher} in OsmAnd-shared against the original in OsmAnd-java, each with a
 * repository for every obf file the tests ship with: the routing maps, which have map sections, and
 * the maps of the search tests; and the real maps {@code OSMAND_AMENITY_SEARCHER_MAPS} names,
 * separated by {@code :}, a travel file among them if it should be searched too.
 *
 * The repositories of java are {@code AmenityIndexRepositoryBinary} of the android app, which needs
 * the app: {@link JavaRepository} makes the same calls to the reader. The copy has
 * {@code BinaryAmenityIndexRepository}, which {@code TravelRepositoryCompatTest} compares with it.
 *
 * For amenities spread over each file, both sides:
 * <ul>
 * <li>search the amenities around it;</li>
 * <li>make the object it stands for: from the amenity itself, from its names typed in a little way
 * off, with and without its type, from an object the renderer could have drawn for it, and from a
 * details object of it, completing the geometry from the map section where it has none;</li>
 * <li>search by the first letters of its name, the nearest ones in a queue of a few, and along a
 * path through it.</li>
 * </ul>
 * With them, each file's amenities merged by osm id and wikidata, the order of the repositories,
 * routes by id, the objects of settlements and streets, and the searches in the background.
 *
 * It writes what java answered, with how to ask it again, to {@code build/amenity-searcher-java.txt};
 * {@code AmenitySearcherTest} in OsmAnd-shared holds the copy to it on Kotlin/Native.
 */
public class AmenitySearcherCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/amenity-searcher-java.txt");
	/** Every how many amenities of a file one is searched around. */
	private static final int STEP = 17;

	private static PrintWriter dump;
	private static final List<File> files = new ArrayList<>();
	private static final List<BinaryMapIndexReader> javaReaders = new ArrayList<>();
	private static final List<net.osmand.shared.binary.BinaryMapIndexReader> copyReaders = new ArrayList<>();
	private static final List<List<Amenity>> javaAmenities = new ArrayList<>();
	private static final List<List<Object>> copyAmenities = new ArrayList<>();
	/** The box the amenities of each file are read from, in 31 tiles: left, right, top, bottom. */
	private static final List<int[]> boxes = new ArrayList<>();
	private static AmenitySearcher java;
	private static net.osmand.shared.search.AmenitySearcher copy;
	private static AmenitySearcher.Settings javaSettings;
	private static Object copySettings;
	private static int compared;
	private static int found;
	private static int detailed;
	private static int completed;

	@BeforeClass
	public static void open() throws Exception {
		String corpus = System.getenv("OSMAND_OBF_CORPUS");
		Assume.assumeTrue("a run over OSMAND_OBF_CORPUS leaves the search files out", corpus == null || corpus.isEmpty());
		SearchApisCompatTest.setUpTypes();
		List<File> all = new ArrayList<>(TestObf.files());
		all.addAll(TestObf.searchFiles());
		int shipped = all.size();
		String maps = System.getenv("OSMAND_AMENITY_SEARCHER_MAPS");
		if (maps != null && !maps.isEmpty()) {
			for (String path : maps.split(":")) {
				all.add(new File(path));
			}
		}
		// of a real map only the amenities around the first town of the first one
		int[] box = {0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE};
		java = new AmenitySearcher(MapPoiTypes.getDefault());
		copy = new net.osmand.shared.search.AmenitySearcher(net.osmand.shared.osm.MapPoiTypes.Companion.getDefault());
		for (File f : all) {
			BinaryMapIndexReader j = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			net.osmand.shared.binary.BinaryMapIndexReader k = new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath());
			files.add(f);
			javaReaders.add(j);
			copyReaders.add(k);
			java.addAmenityRepository(f.getName(), new JavaRepository(f, j));
			copy.addAmenityRepository(f.getName(), new net.osmand.shared.binary.BinaryAmenityIndexRepository(f.getPath(), () -> k));
			if (files.size() == shipped + 1) {
				List<City> towns = j.getCities(null, CityBlocks.CITY_TOWN_TYPE, null, null);
				LatLon c = towns.isEmpty() ? j.getRegionCenter() : towns.get(0).getLocation();
				QuadRect r = MapUtils.calculateLatLonBbox(c.getLatitude(), c.getLongitude(), 3000);
				box = new int[] {MapUtils.get31TileNumberX(r.left), MapUtils.get31TileNumberX(r.right),
						MapUtils.get31TileNumberY(r.top), MapUtils.get31TileNumberY(r.bottom)};
			}
			List<Amenity> ja = j.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
					box[0], box[1], box[2], box[3], -1, null, null, null));
			List<Object> ka = new ArrayList<>(k.searchPoi(net.osmand.shared.binary.SearchRequest.Companion.buildSearchPoiRequest(
					box[0], box[1], box[2], box[3], -1, null, null, null)));
			assertEquals(f.getName() + " amenities", ja.size(), ka.size());
			if (files.size() > shipped) {
				System.out.println("AmenitySearcherCompatTest: " + ja.size() + " amenities of " + f.getName() + " around " + box[0] + "," + box[2]);
			}
			boxes.add(box);
			javaAmenities.add(ja);
			copyAmenities.add(ka);
		}
		javaSettings = new AmenitySearcher.Settings(() -> "en", () -> false, null);
		copySettings = copySettings("en", false);
		dump = new PrintWriter(Files.newBufferedWriter(DUMP.toPath(), StandardCharsets.UTF_8));
		for (Map.Entry<String, String> e : SearchApisCompatTest.poiPhrases.entrySet()) {
			dump.println("X\t" + hex(e.getKey()) + "\t" + hex(e.getValue()));
		}
		for (int f = 0; f < files.size(); f++) {
			dump.println("F\t" + f + "\t" + hex(files.get(f).getPath()) + "\t" + (f < shipped ? "-" : box[0] + "," + box[1] + "," + box[2] + "," + box[3]));
		}
	}

	@AfterClass
	public static void close() throws IOException {
		if (dump != null) {
			dump.close();
		}
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
		for (BinaryMapIndexReader r : javaReaders) {
			r.close();
		}
		for (net.osmand.shared.binary.BinaryMapIndexReader r : copyReaders) {
			r.close();
		}
		System.out.println("AmenitySearcherCompatTest: " + files.size() + " files, " + compared + " answers compared, "
				+ found + " amenities found, " + detailed + " detailed objects made, " + completed
				+ " of them with the geometry of a map object");
		// the other compat tests run in this jvm after it
		files.clear();
		javaReaders.clear();
		copyReaders.clear();
		javaAmenities.clear();
		copyAmenities.clear();
		boxes.clear();
		java = null;
		copy = null;
	}

	/** The answers for amenities spread over each file. */
	@Test
	public void searchesAreTheSame() {
		for (int f = 0; f < files.size(); f++) {
			List<Amenity> ja = javaAmenities.get(f);
			List<Object> ka = copyAmenities.get(f);
			for (int i = 0; i < ja.size(); i += STEP) {
				Amenity a = ja.get(i);
				Object k = ka.get(i);
				LatLon l = a.getLocation();
				Object kl = call(k, "getLocation");
				String at = f + "\t" + i;

				answer("P", at, "", () -> amenities(java.searchAmenities(l, javaSettings)),
						() -> amenities(call(copy, "searchAmenities", kl, copySettings)));
				answer("D", at, "", () -> details(java.searchDetailedObject(new AmenitySearcher.Request(a), javaSettings, null)),
						() -> details(call(copy, "searchDetailedObject", new net.osmand.shared.search.AmenitySearcher.Request(
								(net.osmand.shared.data.MapObject) k), copySettings, null)));
				for (int v = 0; v < NAMES; v++) {
					int variant = v;
					answer("N", at, String.valueOf(v), () -> details(java.searchDetailedObject(javaByNames(a, variant), javaSettings)),
							() -> details(call(copy, "searchDetailedObject", copyByNames(k, variant), copySettings)));
				}
				answer("O", at, "", () -> details(java.searchDetailedObject((Object) a, javaSettings)),
						() -> details(call(copy, "searchDetailedObject", (Object) k, copySettings)));
				answer("B", at, "", () -> details(java.searchDetailedObject(new BaseDetailsObject(a, "en"), javaSettings)),
						() -> details(call(copy, "searchDetailedObject", new net.osmand.shared.data.BaseDetailsObject(k, "en"), copySettings)));
				RenderedObject jd = BaseDetailsObjectCompatTest.drawnJava(a, i);
				net.osmand.shared.data.RenderedObject kd = BaseDetailsObjectCompatTest.drawnCopy((net.osmand.shared.data.Amenity) k, i);
				String spec = BaseDetailsObjectCompatTest.spec(jd);
				answer("R", at, spec, () -> details(java.searchDetailedObject(new AmenitySearcher.Request(jd), javaSettings, null)),
						() -> details(call(copy, "searchDetailedObject", new net.osmand.shared.search.AmenitySearcher.Request(kd), copySettings, null)));
				answer("E", at, spec, () -> details(java.searchDetailedObject(new AmenitySearcher.Request(jd,
								Collections.singletonList(a.toStringEn()), true), javaSettings, null)),
						() -> details(call(copy, "searchDetailedObject", new net.osmand.shared.search.AmenitySearcher.Request(kd,
								Collections.singletonList((String) call(k, "toStringEn")), true), copySettings, null)));
				String name = a.getName();
				if (!name.isEmpty()) {
					String prefix = name.substring(0, Math.min(3, name.length()));
					QuadRect r = MapUtils.calculateLatLonBbox(l.getLatitude(), l.getLongitude(), 5000);
					answer("Q", at, hex(prefix), () -> byFile(java.searchAmenitiesByName(prefix, r.top, r.left, r.bottom, r.right,
									l.getLatitude(), l.getLongitude(), null)),
							() -> byFile(call(copy, "searchAmenitiesByName", prefix, r.top, r.left, r.bottom, r.right,
									l.getLatitude(), l.getLongitude(), null)));
				}
				QuadRect r = MapUtils.calculateLatLonBbox(l.getLatitude(), l.getLongitude(), 1000);
				answer("K", at, "", () -> amenities(java.searchAmenities(BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null,
								r.top, r.left, r.bottom, r.right, -1, true, null, null, null, nearest(l), 5)),
						() -> amenities(call(copy, "searchAmenities", net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER,
								null, r.top, r.left, r.bottom, r.right, -1, true, null, null, null, copyNearest(kl), 5)));
				String category = a.getType().getKeyName();
				QuadRect near = MapUtils.calculateLatLonBbox(l.getLatitude(), l.getLongitude(), FILTERED_RADIUS);
				for (int v = 0; v < FILTERED; v++) {
					int variant = v;
					answer("V", at, String.valueOf(v), () -> ids(javaFiltered(variant, category, near)),
							() -> ids(copyFiltered(variant, category, near)));
				}
				answer("W", at, "", () -> sorted(amenities(java.searchAmenitiesOnThePath(path(l), 100,
								BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER, null))),
						() -> sorted(amenities(call(copy, "searchAmenitiesOnThePath", copyPath(l), 100.0,
								net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null))));
			}
		}
		assertTrue("answers compared: " + compared, compared > 10000);
		assertTrue("detailed objects made: " + detailed, detailed > 1000);
	}

	/** The amenities of each file merged by osm id and wikidata, and those of all files at once. */
	@Test
	public void mergesAreTheSame() throws IOException {
		List<Amenity> jall = new ArrayList<>();
		List<Object> kall = new ArrayList<>();
		for (int f = 0; f < files.size(); f++) {
			// merging changes the amenities it merges into: fresh ones
			List<Amenity> ja = fresh(f);
			List<Object> ka = copyFresh(f);
			jall.addAll(fresh(f));
			kall.addAll(copyFresh(f));
			answer("M", f + "\t-1", "", () -> amenities(java.mergeAmenities(ja, javaSettings)),
					() -> amenities(call(copy, "mergeAmenities", ka, copySettings)));
		}
		answer("M", "-1\t-1", "", () -> amenities(java.mergeAmenities(jall, javaSettings)),
				() -> amenities(call(copy, "mergeAmenities", kall, copySettings)));
	}

	/**
	 * Amenities made up to have an osm id of one merged amenity and the wikidata of another, which the
	 * files do not have: the two are merged into the first, and what comes to the second after them
	 * goes to the first too.
	 */
	@Test
	public void mergesOfOneIdAndAnotherWikidataAreTheSame() {
		long[][] made = {{1, 1}, {2, 2}, {1, 2}, {2, 0}, {3, 2}, {0, 1}, {4, 3}, {3, 4}};
		List<Amenity> j = new ArrayList<>();
		List<Object> k = new ArrayList<>();
		for (int i = 0; i < made.length; i++) {
			Amenity a = new Amenity();
			net.osmand.shared.data.Amenity c = new net.osmand.shared.data.Amenity();
			for (Object o : new Object[] {a, c}) {
				call(o, "setId", made[i][0] == 0 ? -2L * (i + 1) : made[i][0] << 1);
				call(o, "setType", o == a ? MapPoiTypes.getDefault().getOtherPoiCategory()
						: net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getOtherPoiCategory());
				call(o, "setSubType", "shop");
				call(o, "setName", "Shop " + i);
				call(o, "setLocation", 52.5 + i / 1000.0, 13.4);
				if (made[i][1] != 0) {
					call(o, "setAdditionalInfo", "wikidata", "Q" + made[i][1]);
				}
				call(o, "setAdditionalInfo", "opening_hours", "Mo-Fr " + i + ":00-18:00");
			}
			j.add(a);
			k.add(c);
		}
		answer("Y", "-1\t-1", "", () -> amenities(java.mergeAmenities(j, javaSettings)),
				() -> amenities(call(copy, "mergeAmenities", k, copySettings)));
	}

	/** Which repositories are searched, in which order; routes by id; settlements and streets. */
	@Test
	public void repositoriesAndAddressesAreTheSame() throws IOException {
		answer("G", "-1\t-1", "0", () -> repositories(java.getAmenityRepositories(true, null)),
				() -> repositories(call(copy, "getAmenityRepositories", true, null)));
		answer("G", "-1\t-1", "1", () -> repositories(java.getAmenityRepositories(false, null)),
				() -> repositories(call(copy, "getAmenityRepositories", false, null)));
		Set<String> routeIds = new LinkedHashSet<>();
		for (List<Amenity> ja : javaAmenities) {
			for (Amenity a : ja) {
				String id = a.getAdditionalInfo(Amenity.ROUTE_ID);
				if (id != null && routeIds.size() < 20) {
					routeIds.add(id);
				}
			}
		}
		routeIds.add("OSM0");
		for (String id : routeIds) {
			answer("T", "-1\t-1", hex(id), () -> sorted(amenities(java.searchRoutePartOf(id))),
					() -> sorted(amenities(call(copy, "searchRoutePartOf", id))));
		}
		String members = String.join(" ", routeIds);
		answer("U", "-1\t-1", hex(members), () -> routes(java.searchRouteMembers(members)),
				() -> routes(call(copy, "searchRouteMembers", members)));
		for (int f = 0; f < files.size(); f++) {
			List<City> jc = javaReaders.get(f).getCities(null, CityBlocks.CITY_TOWN_TYPE, null, null);
			List<net.osmand.shared.data.City> kc = copyReaders.get(f).getCities(null, net.osmand.shared.binary.CityBlocks.CITY_TOWN_TYPE);
			assertEquals(files.get(f).getName() + " settlements", jc.size(), kc.size());
			for (int c = 0; c < jc.size(); c += 7) {
				City j = jc.get(c);
				net.osmand.shared.data.City k = kc.get(c);
				javaReaders.get(f).preloadStreets(j, null, null);
				copyReaders.get(f).preloadStreets(k, null);
				answer("C", f + "\t" + c, "", () -> details(java.searchDetailedObject((Object) j, javaSettings)),
						() -> details(call(copy, "searchDetailedObject", k, copySettings)));
				List<Street> js = j.getStreets();
				List<?> ks = (List<?>) call(k, "getStreets");
				if (!js.isEmpty()) {
					Street s = js.get(0);
					Object t = ks.get(0);
					answer("S", f + "\t" + c, "", () -> details(java.searchDetailedObject((Object) s, javaSettings)),
							() -> details(call(copy, "searchDetailedObject", t, copySettings)));
				}
			}
		}
	}

	/** The searches in the background answer as the ones at once. */
	@Test
	public void searchesInTheBackgroundAreTheSame() throws Exception {
		for (int f = 0; f < files.size(); f += 5) {
			List<Amenity> ja = javaAmenities.get(f);
			if (ja.isEmpty()) {
				continue;
			}
			Amenity a = ja.get(0);
			Object k = copyAmenities.get(f).get(0);
			String m = files.get(f).getName();
			Object[] results = new Object[6];
			CountDownLatch done = new CountDownLatch(6);
			java.searchDetailedAmenityAsync(new AmenitySearcher.Request(a), javaSettings, res -> {
				results[0] = amenity(res);
				done.countDown();
				return true;
			});
			call(copy, "searchDetailedAmenityAsync", new net.osmand.shared.search.AmenitySearcher.Request(
					(net.osmand.shared.data.MapObject) k), copySettings, callback(res -> {
				results[1] = amenity(res);
				done.countDown();
			}));
			java.searchDetailedObjectAsync(a, javaSettings, res -> {
				results[2] = res instanceof BaseDetailsObject ? details(res) : "object";
				done.countDown();
				return true;
			});
			call(copy, "searchDetailedObjectAsync", k, copySettings, callback(res -> {
				results[3] = res instanceof net.osmand.shared.data.BaseDetailsObject ? details(res) : "object";
				done.countDown();
			}));
			RenderedObject jd = BaseDetailsObjectCompatTest.drawnJava(a, 1);
			net.osmand.shared.data.RenderedObject kd = BaseDetailsObjectCompatTest.drawnCopy((net.osmand.shared.data.Amenity) k, 1);
			java.searchBaseDetailedObjectAsync(jd, javaSettings, res -> {
				results[4] = details(res);
				done.countDown();
				return true;
			}, null);
			call(copy, "searchBaseDetailedObjectAsync", kd, copySettings, callback(res -> {
				results[5] = details(res);
				done.countDown();
			}), null);
			assertTrue(m, done.await(60, TimeUnit.SECONDS));
			assertEquals(m, results[0], results[1]);
			assertEquals(m, results[2], results[3]);
			assertEquals(m, results[4], results[5]);
			compared += 3;
		}
	}

	private static void answer(String kind, String at, String extra, Supplier<String> javaAnswer, Supplier<String> copyAnswer) {
		String j = outcome(javaAnswer);
		String k = outcome(copyAnswer);
		if (!j.equals(k)) {
			String[] jl = j.split("\n");
			String[] kl = k.split("\n");
			System.out.println("MISMATCH " + kind + " " + at + " " + extra + ": " + jl.length + " / " + kl.length + " lines");
			for (int i = 0; i < Math.max(jl.length, kl.length); i++) {
				String x = i < jl.length ? jl[i] : "-";
				String y = i < kl.length ? kl[i] : "-";
				if (!x.equals(y)) {
					System.out.println("  J " + x + "\n  K " + y);
				}
			}
		}
		assertEquals(kind + " " + at.replace('\t', ' ') + " " + extra, j, k);
		dump.println(kind + "\t" + at + "\t" + (extra.isEmpty() ? "-" : extra) + "\t" + hex(j));
		compared++;
	}

	private static String outcome(Supplier<String> answer) {
		try {
			return answer.get();
		} catch (Throwable t) {
			Throwable cause = t;
			while ((cause instanceof AssertionError || cause instanceof java.lang.reflect.InvocationTargetException)
					&& cause.getCause() != null) {
				cause = cause.getCause();
			}
			return "threw:" + cause.getClass().getSimpleName();
		}
	}

	// the requests

	/** How many ways the names of an amenity are asked for. */
	static final int NAMES = 4;

	/** How many ways the amenities around one are searched with filters, {@link #javaFiltered}. */
	static final int FILTERED = 6;
	/** How far around the amenity the filters search, in metres. */
	static final int FILTERED_RADIUS = 300;
	/** The zoom the search with a filter of a category thins its amenities out for. */
	static final int FILTERED_ZOOM = 15;

	/**
	 * A request by the names of [a], 20 m north of it, as a link to a place asks: its name, a name of
	 * it in another language or its english name, the name of its type with the type, and its name
	 * with its osm id.
	 */
	private static AmenitySearcher.Request javaByNames(Amenity a, int variant) {
		LatLon l = new LatLon(a.getLocation().getLatitude() + 0.00018, a.getLocation().getLongitude());
		switch (variant) {
			case 0:
				return new AmenitySearcher.Request(Collections.singletonList(a.getName()), l, null, -1L, null);
			case 1:
				Collection<String> names = a.getNamesMap(true).values();
				return new AmenitySearcher.Request(Collections.singletonList(names.isEmpty() ? a.getName() : names.iterator().next()),
						l, null, -1L, null);
			case 2:
				AbstractPoiType t = a.getSubType() == null ? null : MapPoiTypes.getDefault().getAnyPoiTypeByKey(a.getSubType());
				return new AmenitySearcher.Request(Collections.singletonList(t == null ? "-" : t.getTranslation()), l, null, -1L,
						a.getSubType());
			default:
				return new AmenitySearcher.Request(Collections.singletonList(a.getName()), l, a.getWikidata(), a.getOsmId(), null);
		}
	}

	private static Object copyByNames(Object k, int variant) {
		net.osmand.shared.data.Amenity a = (net.osmand.shared.data.Amenity) k;
		net.osmand.shared.data.KLatLon l = new net.osmand.shared.data.KLatLon(a.getLocation().getLatitude() + 0.00018,
				a.getLocation().getLongitude());
		switch (variant) {
			case 0:
				return new net.osmand.shared.search.AmenitySearcher.Request(Collections.singletonList(a.getName()), l, null, -1L, null);
			case 1:
				Collection<String> names = a.getNamesMap(true).values();
				return new net.osmand.shared.search.AmenitySearcher.Request(
						Collections.singletonList(names.isEmpty() ? a.getName() : names.iterator().next()), l, null, -1L, null);
			case 2:
				net.osmand.shared.osm.AbstractPoiType t = a.getSubType() == null ? null
						: net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getAnyPoiTypeByKey(a.getSubType());
				return new net.osmand.shared.search.AmenitySearcher.Request(
						Collections.singletonList(t == null ? "-" : t.getTranslation()), l, null, -1L, a.getSubType());
			default:
				return new net.osmand.shared.search.AmenitySearcher.Request(Collections.singletonList(a.getName()), l,
						a.getWikidata(), a.getOsmId(), null);
		}
	}

	/** The nearest to [l] first. */
	private static Comparator<Amenity> nearest(LatLon l) {
		return Comparator.comparingDouble((Amenity a) -> -MapUtils.getDistance(a.getLocation(), l));
	}

	private static Comparator<Object> copyNearest(Object l) {
		net.osmand.shared.data.KLatLon at = (net.osmand.shared.data.KLatLon) l;
		return Comparator.comparingDouble((Object a) -> -net.osmand.shared.util.KMapUtils.INSTANCE.getDistance(
				((net.osmand.shared.data.Amenity) a).getLocation(), at));
	}

	/**
	 * The amenities in [r] searched with: 0 a filter of [category], 1 an empty filter and an additional
	 * filter, 2 an empty filter alone, which would take every type if asked, 3 one repository in two,
	 * 4 a matcher that stops at the third found, 5 the world maps only.
	 */
	private static List<Amenity> javaFiltered(int variant, String category, QuadRect r) {
		SearchPoiTypeFilter all = BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
		SearchPoiTypeFilter empty = new SearchPoiTypeFilter() {
			@Override
			public boolean accept(net.osmand.osm.PoiCategory type, String subcategory) {
				return false;
			}

			@Override
			public boolean isEmpty() {
				return true;
			}
		};
		SearchPoiAdditionalFilter additional = new SearchPoiAdditionalFilter() {
			@Override
			public boolean accept(net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType poiSubType, String value) {
				return true;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public String getIconResource() {
				return null;
			}
		};
		int[] published = {0};
		ResultMatcher<Amenity> third = new ResultMatcher<>() {
			@Override
			public boolean publish(Amenity object) {
				return ++published[0] <= 3;
			}

			@Override
			public boolean isCancelled() {
				return published[0] >= 3;
			}
		};
		switch (variant) {
			case 0:
				return java.searchAmenities(new SearchPoiTypeFilter() {
					@Override
					public boolean accept(net.osmand.osm.PoiCategory type, String subcategory) {
						return type.getKeyName().equals(category);
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, null, r.top, r.left, r.bottom, r.right, FILTERED_ZOOM, true, null, null);
			case 1:
				return java.searchAmenities(empty, additional, r.top, r.left, r.bottom, r.right, -1, true, null, null);
			case 2:
				return java.searchAmenities(new SearchPoiTypeFilter() {
					@Override
					public boolean accept(net.osmand.osm.PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return true;
					}
				}, null, r.top, r.left, r.bottom, r.right, -1, true, null, null);
			case 3:
				return java.searchAmenities(all, null, r.top, r.left, r.bottom, r.right, -1, true, null, null,
						repo -> repo.getFile().getName().length() % 2 == 0, null, -1);
			case 4:
				return java.searchAmenities(all, null, r.top, r.left, r.bottom, r.right, -1, true, null, third);
			default:
				return java.searchWorldMapAmenities(all, r, true, null, null);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Object> copyFiltered(int variant, String category, QuadRect r) {
		net.osmand.shared.binary.SearchPoiTypeFilter all = net.osmand.shared.binary.SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER;
		net.osmand.shared.binary.SearchPoiTypeFilter empty = new net.osmand.shared.binary.SearchPoiTypeFilter() {
			@Override
			public boolean accept(net.osmand.shared.osm.PoiCategory type, String subcategory) {
				return false;
			}

			@Override
			public boolean isEmpty() {
				return true;
			}
		};
		net.osmand.shared.binary.SearchPoiAdditionalFilter additional = new net.osmand.shared.binary.SearchPoiAdditionalFilter() {
			@Override
			public boolean accept(net.osmand.shared.binary.PoiSubType poiSubType, String value) {
				return true;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public String getIconResource() {
				return null;
			}
		};
		int[] published = {0};
		net.osmand.shared.binary.ResultMatcher<Object> third = new net.osmand.shared.binary.ResultMatcher<>() {
			@Override
			public boolean publish(Object object) {
				return ++published[0] <= 3;
			}

			@Override
			public boolean isCancelled() {
				return published[0] >= 3;
			}
		};
		net.osmand.shared.data.KQuadRect kr = new net.osmand.shared.data.KQuadRect(r.left, r.top, r.right, r.bottom);
		switch (variant) {
			case 0:
				return (List<Object>) call(copy, "searchAmenities", new net.osmand.shared.binary.SearchPoiTypeFilter() {
					@Override
					public boolean accept(net.osmand.shared.osm.PoiCategory type, String subcategory) {
						return type.getKeyName().equals(category);
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, null, r.top, r.left, r.bottom, r.right, FILTERED_ZOOM, true, null, null);
			case 1:
				return (List<Object>) call(copy, "searchAmenities", empty, additional, r.top, r.left, r.bottom, r.right, -1, true, null, null);
			case 2:
				return (List<Object>) call(copy, "searchAmenities", new net.osmand.shared.binary.SearchPoiTypeFilter() {
					@Override
					public boolean accept(net.osmand.shared.osm.PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return true;
					}
				}, null, r.top, r.left, r.bottom, r.right, -1, true, null, null);
			case 3:
				return (List<Object>) call(copy, "searchAmenities", all, null, r.top, r.left, r.bottom, r.right, -1, true, null, null,
						copyFunction(repo -> ((net.osmand.shared.binary.AmenityIndexRepository) repo).getFile().name().length() % 2 == 0),
						null, -1);
			case 4:
				return (List<Object>) call(copy, "searchAmenities", all, null, r.top, r.left, r.bottom, r.right, -1, true, null, third);
			default:
				return (List<Object>) call(copy, "searchWorldMapAmenities", all, kr, true, null, null);
		}
	}

	/** Three points 200 m apart, the middle one on [l]. */
	private static List<Location> path(LatLon l) {
		List<Location> path = new ArrayList<>();
		for (int p = -1; p <= 1; p++) {
			path.add(new Location("", l.getLatitude() + p * 0.0018, l.getLongitude() + p * 0.0018));
		}
		return path;
	}

	private static List<Object> copyPath(LatLon l) {
		List<Object> path = new ArrayList<>();
		for (int p = -1; p <= 1; p++) {
			path.add(new net.osmand.shared.data.KLocation("", l.getLatitude() + p * 0.0018, l.getLongitude() + p * 0.0018));
		}
		return path;
	}

	private static List<Amenity> fresh(int f) throws IOException {
		int[] b = boxes.get(f);
		return javaReaders.get(f).searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(b[0], b[1], b[2], b[3], -1, null, null, null));
	}

	private static List<Object> copyFresh(int f) {
		int[] b = boxes.get(f);
		return new ArrayList<>(copyReaders.get(f).searchPoi(net.osmand.shared.binary.SearchRequest.Companion.buildSearchPoiRequest(
				b[0], b[1], b[2], b[3], -1, null, null, null)));
	}

	// the lines of the answers, the same for both sides: doubles as their bits after an @

	/** Which amenities, and where: what the amenities are like the other answers hold. */
	private static String ids(Object list) {
		StringBuilder s = new StringBuilder();
		for (Object a : (Collection<?>) list) {
			s.append('A').append(call(a, "getId")).append(':').append(location(call(a, "getLocation"))).append('\n');
			found++;
		}
		return s.toString();
	}

	static String amenities(Object list) {
		if (list == null) {
			return "null";
		}
		StringBuilder s = new StringBuilder();
		for (Object a : (Collection<?>) list) {
			s.append(amenity(a)).append('\n');
			found++;
		}
		return s.toString();
	}

	/**
	 * The amenities each file found, the files in their order and the amenities of one in the order of
	 * their lines: the name index hands out the ones in one block in the order of java's hash map.
	 */
	static String byFile(Object list) {
		StringBuilder s = new StringBuilder();
		List<String> file = new ArrayList<>();
		Object region = null;
		for (Object a : (Collection<?>) list) {
			Object r = call(a, "getRegionName");
			if (!file.isEmpty() && !Objects.equals(r, region)) {
				Collections.sort(file);
				s.append(String.join("", file));
				file.clear();
			}
			region = r;
			file.add(amenity(a) + "\n");
			found++;
		}
		Collections.sort(file);
		return s.append(String.join("", file)).toString();
	}

	static String amenity(Object a) {
		if (a == null) {
			return "null";
		}
		Object type = call(a, "getType");
		StringBuilder s = new StringBuilder("A").append(call(a, "getId")).append(':')
				.append(type == null ? "-" : call(type, "getKeyName")).append(':').append(call(a, "getSubType")).append(':')
				.append(call(a, "getName")).append(':').append(location(call(a, "getLocation"))).append(':')
				.append(call(a, "getRegionName")).append(':');
		Map<String, String> info = new TreeMap<>();
		for (Object key : (Collection<?>) call(a, "getAdditionalInfoKeys")) {
			info.put((String) key, (String) call(a, "getAdditionalInfo", key));
		}
		s.append(info).append(':').append(new TreeMap<>((Map<?, ?>) call(a, "getNamesMap", true))).append(':')
				.append(geometry(call(a, "getX"), call(a, "getY")));
		return s.toString();
	}

	static String details(Object b) {
		if (b == null) {
			return "null";
		}
		detailed++;
		StringBuilder s = new StringBuilder((boolean) call(b, "isObjectFull") ? "F" : (boolean) call(b, "isObjectCombined") ? "C"
				: (boolean) call(b, "isObjectEmpty") ? "E" : "?");
		s.append('|').append(call(b, "getResourceType")).append('|').append(call(b, "getLang")).append('|')
				.append(call(b, "getPointsLength")).append('|');
		for (Object o : (List<?>) call(b, "getObjects")) {
			String n = o.getClass().getSimpleName();
			if (n.equals("Amenity")) {
				s.append(amenity(o));
			} else {
				s.append(n).append(':').append(call(o, "getId")).append(':').append(call(o, "getName")).append(':')
						.append(location(call(o, "getLocation")));
			}
			s.append('\n');
		}
		Object synthetic = call(b, "getSyntheticAmenity");
		if ((int) call(call(synthetic, "getX"), "size") > 0) {
			completed++;
		}
		return s.append('|').append(amenity(synthetic)).toString();
	}

	/** The points of a geometry: how many, and a sum of them. */
	private static String geometry(Object xs, Object ys) {
		int n = (int) call(xs, "size");
		long sum = 0;
		for (int p = 0; p < n; p++) {
			sum = sum * 31 + (int) call(xs, "get", p);
			sum = sum * 31 + (int) call(ys, "get", p);
		}
		return n + "#" + sum;
	}

	private static String location(Object l) {
		if (l == null) {
			return "null";
		}
		return "@" + SearchPhraseCompatTest.bits((Double) call(l, "getLatitude")) + ",@"
				+ SearchPhraseCompatTest.bits((Double) call(l, "getLongitude"));
	}

	private static String repositories(Object list) {
		StringBuilder s = new StringBuilder();
		for (Object r : (Collection<?>) list) {
			s.append(r instanceof JavaRepository ? ((JavaRepository) r).getFile().getName()
					: (String) call(call(r, "getFile"), "name")).append('\n');
		}
		return s.toString();
	}

	private static String routes(Object map) {
		StringBuilder s = new StringBuilder();
		for (Map.Entry<?, ?> e : new TreeMap<>((Map<?, ?>) map).entrySet()) {
			s.append(e.getKey()).append('=').append(e.getValue() == null ? "null" : sorted(amenities(e.getValue()))).append('\n');
		}
		return s.toString();
	}

	private static String sorted(String lines) {
		List<String> l = new ArrayList<>(List.of(lines.split("\n")));
		Collections.sort(l);
		return String.join("\n", l);
	}

	// the copy's kotlin lambdas, which OsmAnd-java sees only when it runs

	private static Object copySettings(String lang, boolean transliterate) {
		try {
			Class<?> f0 = SearchApisCompatTest.FUNCTION0;
			Class<?> f1 = Class.forName("kotlin.jvm.functions.Function1");
			return net.osmand.shared.search.AmenitySearcher.Settings.class.getConstructor(f0, f0, f1)
					.newInstance(function(f0, args -> lang), function(f0, args -> transliterate), null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Object callback(java.util.function.Consumer<Object> c) {
		try {
			return function(Class.forName("kotlin.jvm.functions.Function1"), args -> {
				c.accept(args[0]);
				return Boolean.TRUE;
			});
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static Object copyFunction(java.util.function.Function<Object, Object> f) {
		try {
			return function(Class.forName("kotlin.jvm.functions.Function1"), args -> f.apply(args[0]));
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static Object function(Class<?> type, java.util.function.Function<Object[], Object> invoke) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
			switch (method.getName()) {
				case "invoke":
					return invoke.apply(args);
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				default:
					return "function";
			}
		});
	}

	/**
	 * {@code AmenityIndexRepositoryBinary} of the android app, which needs the app to be made: the same
	 * calls to the reader, without the log.
	 */
	static final class JavaRepository implements AmenityIndexRepository {
		private final File file;
		private final BinaryMapIndexReader reader;

		JavaRepository(File file, BinaryMapIndexReader reader) {
			this.file = file;
			this.reader = reader;
		}

		@Override
		public void close() {
		}

		@Override
		public boolean checkContains(double latitude, double longitude) {
			int x31 = MapUtils.get31TileNumberX(longitude);
			int y31 = MapUtils.get31TileNumberY(latitude);
			return reader.containsPoiData(x31, y31, x31, y31);
		}

		@Override
		public boolean checkContainsInt(int top31, int left31, int bottom31, int right31) {
			return reader.containsPoiData(left31, top31, right31, bottom31);
		}

		@Override
		public synchronized List<Amenity> searchAmenitiesByName(int x, int y, int l, int t, int r, int b, String query,
				ResultMatcher<Amenity> resulMatcher) {
			List<Amenity> amenities = Collections.emptyList();
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(x, y, query, l, r, t, b, resulMatcher);
			try {
				amenities = reader.searchPoiByName(req);
			} catch (Exception e) {
				// logged by the app
			}
			return amenities;
		}

		@Override
		public synchronized List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom,
				SearchPoiTypeFilter filter, SearchPoiAdditionalFilter additionalFilter, ResultMatcher<Amenity> matcher,
				PriorityQueue<Amenity> priorityQueue, int priorityQueueLimit) {
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom,
					filter, additionalFilter, matcher);
			req.setPriorityQueue(priorityQueue, priorityQueueLimit);
			List<Amenity> result = null;
			try {
				result = reader.searchPoi(req);
			} catch (Exception e) {
				// logged by the app
			}
			return result;
		}

		@Override
		public synchronized List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius,
				SearchPoiTypeFilter filter, ResultMatcher<Amenity> matcher) {
			List<Amenity> result = null;
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(locations, radius, filter, matcher);
			try {
				result = reader.searchPoi(req);
			} catch (Exception e) {
				return result;
			}
			return result;
		}

		@Override
		public File getFile() {
			return file;
		}

		@Override
		public boolean isWorldMap() {
			String fileName = getFile().getName().toLowerCase();
			return fileName.startsWith("world_") || fileName.contains("basemap");
		}

		@Override
		public List<PoiRegion> getReaderPoiIndexes() {
			return reader.getPoiIndexes();
		}

		@Override
		public synchronized void searchMapIndex(SearchRequest<BinaryMapDataObject> searchRequest) {
			try {
				reader.searchMapIndex(searchRequest);
			} catch (Exception e) {
				// logged by the app
			}
		}

		@Override
		public synchronized void searchPoi(SearchRequest<Amenity> searchRequest) {
			try {
				reader.searchPoi(searchRequest);
			} catch (Exception e) {
				// logged by the app
			}
		}

		@Override
		public synchronized List<Amenity> searchPoiByName(SearchRequest<Amenity> searchRequest) {
			try {
				return reader.searchPoiByName(searchRequest);
			} catch (Exception e) {
				// logged by the app
			}
			return new ArrayList<>();
		}

		@Override
		public boolean isPoiSectionIntersects(SearchRequest<?> searchRequest) {
			for (PoiRegion index : getReaderPoiIndexes()) {
				if (searchRequest.intersects(index.getLeft31(), index.getTop31(), index.getRight31(), index.getBottom31())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isMapSectionIntersects(SearchRequest<?> searchRequest) {
			return reader.containsMapData(searchRequest.getLeft(), searchRequest.getTop(), searchRequest.getRight(),
					searchRequest.getBottom(), searchRequest.getZoom());
		}

		@Override
		public String toString() {
			return getFile().getName();
		}
	}
}
