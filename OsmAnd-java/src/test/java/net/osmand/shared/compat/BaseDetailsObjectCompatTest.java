package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * {@link net.osmand.shared.data.BaseDetailsObject} and {@link net.osmand.shared.data.RenderedObject}
 * are copies of {@link BaseDetailsObject} and {@link RenderedObject}. This builds both from the same
 * objects and compares the amenity each makes of them, field by field, with the objects in the
 * order each keeps them.
 *
 * The objects are the amenities of the obf files of the search tests, read by both readers:
 * <ul>
 * <li>each on its own, in several languages;</li>
 * <li>those that share an osm id or a wikidata id, across the files, the way the search unites
 * them;</li>
 * <li>neighbours in a file, which need not have anything in common, also as if they came from a
 * basemap or a travel file, where the language of an article decides what is copied;</li>
 * <li>objects the renderer could have drawn, made of their tags, alone and with the amenity;</li>
 * <li>settlements, streets and houses with an amenity next to them.</li>
 * </ul>
 * It writes a part of what java made, with how to make it again, to
 * {@code build/details-objects-java.txt}; {@code BaseDetailsObjectTest} in commonTest holds the copy to
 * it on Kotlin/Native.
 */
public class BaseDetailsObjectCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File PHRASES = new File("src/test/resources/phrases/en/phrases.xml");
	private static final String[] LANGS = {"en", "de", "", null};
	private static final File DUMP = new File("build/details-objects-java.txt");
	private static PrintWriter dump;

	private static final List<File> files = new ArrayList<>();
	private static final List<List<Amenity>> javaAmenities = new ArrayList<>();
	private static final List<List<net.osmand.shared.data.Amenity>> copyAmenities = new ArrayList<>();
	private static int compared;
	private static int unitedGroups;
	private static int drawn;
	private static int addresses;

	@BeforeClass
	public static void read() throws Exception {
		String corpus = System.getenv("OSMAND_OBF_CORPUS");
		Assume.assumeTrue("a run over OSMAND_OBF_CORPUS leaves the search files out", corpus == null || corpus.isEmpty());
		if (SearchApisCompatTest.poiPhrases.isEmpty()) {
			for (Map.Entry<String, String> e : Algorithms.parseStringsXml(PHRASES).entrySet()) {
				if (e.getKey().startsWith("poi_")) {
					SearchApisCompatTest.poiPhrases.put(e.getKey(), e.getValue());
				}
			}
		}
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator());
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().setPoiTranslator(new SearchApisCompatTest.CopyTranslator());
		dump = new PrintWriter(Files.newBufferedWriter(DUMP.toPath(), StandardCharsets.UTF_8));
		for (Map.Entry<String, String> e : SearchApisCompatTest.poiPhrases.entrySet()) {
			dump.println("X\t" + SearchPhraseCompatTest.hex(e.getKey()) + "\t" + SearchPhraseCompatTest.hex(e.getValue()));
		}
		for (File f : TestObf.searchFiles()) {
			BinaryMapIndexReader java = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			net.osmand.shared.binary.BinaryMapIndexReader copy = new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath());
			try {
				List<Amenity> j = java.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null, null));
				List<net.osmand.shared.data.Amenity> k = copy.searchPoi(net.osmand.shared.binary.SearchRequest.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, null, null, null));
				assertEquals(f.getName() + " amenities", j.size(), k.size());
				files.add(f);
				javaAmenities.add(j);
				copyAmenities.add(k);
			} finally {
				java.close();
				copy.close();
			}
		}
		assertTrue("search obf files: " + files.size(), files.size() > 50);
	}

	@AfterClass
	public static void reset() {
		if (dump != null) {
			dump.close();
		}
		// the names of the poi types were this test's
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
		System.out.println("BaseDetailsObjectCompatTest: " + compared + " objects compared, " + unitedGroups
				+ " groups of amenities with one osm id or wikidata, " + drawn + " drawn objects, "
				+ addresses + " addresses");
	}

	@Test
	public void singleAmenitiesAreTheSame() {
		for (int f = 0; f < files.size(); f++) {
			List<Amenity> j = javaAmenities.get(f);
			List<net.osmand.shared.data.Amenity> k = copyAmenities.get(f);
			for (int i = 0; i < j.size(); i++) {
				for (String lang : LANGS) {
					String m = files.get(f).getName() + " " + j.get(i) + " " + lang;
					assertDetails(m, new BaseDetailsObject(j.get(i), lang), new net.osmand.shared.data.BaseDetailsObject(k.get(i), lang),
							List.of(j.get(i)), List.of(k.get(i)));
				}
				assertEquals("lang of " + j.get(i), BaseDetailsObject.getLangForTravel(j.get(i)),
						net.osmand.shared.data.BaseDetailsObject.Companion.getLangForTravel(k.get(i)));
				if (i % 4 == 0) {
					dump.println("S\t" + name(f) + "\t" + i + "\t" + esc(describe(new BaseDetailsObject(j.get(i), "en"), List.of(j.get(i)))));
				}
			}
		}
	}

	/** Amenities of all the files that the search would unite: one osm id or one wikidata id. */
	@Test
	public void unitedAmenitiesAreTheSame() {
		Map<Long, Integer> byOsmId = new HashMap<>();
		Map<String, Integer> byWikidata = new HashMap<>();
		List<List<Amenity>> jgroups = new ArrayList<>();
		List<List<net.osmand.shared.data.Amenity>> kgroups = new ArrayList<>();
		List<List<String>> members = new ArrayList<>();
		for (int f = 0; f < files.size(); f++) {
			for (int i = 0; i < javaAmenities.get(f).size(); i++) {
				Amenity a = javaAmenities.get(f).get(i);
				Long osmId = a.getOsmId();
				String wikidata = a.getWikidata();
				if (osmId != null && osmId < 0 || a.isRouteTrack()) {
					osmId = null;
				}
				if (a.isRouteTrack()) {
					wikidata = null;
				}
				Integer group = osmId == null ? null : byOsmId.get(osmId);
				if (group == null && wikidata != null) {
					group = byWikidata.get(wikidata);
				}
				if (group == null) {
					group = jgroups.size();
					jgroups.add(new ArrayList<>());
					kgroups.add(new ArrayList<>());
					members.add(new ArrayList<>());
				}
				jgroups.get(group).add(a);
				members.get(group).add(name(f) + ":" + i);
				kgroups.get(group).add(copyAmenities.get(f).get(i));
				if (osmId != null) {
					byOsmId.put(osmId, group);
				}
				if (wikidata != null) {
					byWikidata.put(wikidata, group);
				}
			}
		}
		for (int g = 0; g < jgroups.size(); g++) {
			if (jgroups.get(g).size() > 1) {
				assertGroup("united " + jgroups.get(g), jgroups.get(g), kgroups.get(g), null,
						"U\t" + String.join(",", members.get(g)), "en");
				unitedGroups++;
			}
		}
		assertTrue("united groups: " + unitedGroups, unitedGroups > 10);
	}

	/**
	 * Two to four neighbours of a file, as they are, as if read from a basemap, as if read from a
	 * travel file in english or german, with the first of them in the category of placeholders, and
	 * with the first of them in no region and the second with an icon and an elo.
	 */
	@Test
	public void neighboursAreTheSame() {
		String[] regions = {null, "World_basemap", "Wikivoyage_travel", "other", "icons"};
		for (int f = 0; f < files.size(); f++) {
			List<Amenity> j = javaAmenities.get(f);
			List<net.osmand.shared.data.Amenity> k = copyAmenities.get(f);
			for (int i = 0; i + 4 <= j.size(); i += 5) {
				for (int size = 2; size <= 4; size++) {
					for (String region : regions) {
						List<Amenity> jgroup = new ArrayList<>();
						List<net.osmand.shared.data.Amenity> kgroup = new ArrayList<>();
						for (int n = 0; n < size; n++) {
							Amenity ja = copyOf(j.get(i + n));
							net.osmand.shared.data.Amenity ka = copyOf(k.get(i + n));
							if ("other".equals(region)) {
								if (n == 0) {
									ja.setType(MapPoiTypes.getDefault().getOtherPoiCategory());
									ka.setType(net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getOtherPoiCategory());
								}
							} else if ("icons".equals(region)) {
								if (n == 0) {
									ja.setRegionName(null);
									ka.setRegionName(null);
								} else if (n == 1) {
									ja.setMapIconName("tourism_museum");
									ka.setMapIconName("tourism_museum");
									ja.setAdditionalInfo(Amenity.TRAVEL_ELO, "1234");
									ka.setAdditionalInfo(Amenity.TRAVEL_ELO, "1234");
								}
							} else if (region != null) {
								ja.setRegionName(region);
								ka.setRegionName(region);
								String langTag = Amenity.LANG_YES + ":" + (n % 2 == 0 ? "de" : "en");
								ja.setAdditionalInfo(langTag, "yes");
								ka.setAdditionalInfo(langTag, "yes");
							}
							jgroup.add(ja);
							kgroup.add(ka);
						}
						assertGroup(files.get(f).getName() + " " + i + "+" + size + " " + region, jgroup, kgroup,
								"other".equals(region) || "icons".equals(region) ? null : region,
								i % 60 == 0 ? "N\t" + name(f) + "\t" + i + "\t" + size + "\t" + region : null, "de");
					}
				}
			}
		}
	}

	/** What the renderer could have drawn for an amenity, alone, with the amenity, and merged. */
	@Test
	public void drawnObjectsAreTheSame() {
		for (int f = 0; f < files.size(); f++) {
			List<Amenity> j = javaAmenities.get(f);
			List<net.osmand.shared.data.Amenity> k = copyAmenities.get(f);
			for (int i = 0; i < j.size(); i += 3) {
				String m = files.get(f).getName() + " drawn " + j.get(i);
				RenderedObject jd = drawnJava(j.get(i), i);
				net.osmand.shared.data.RenderedObject kd = drawnCopy(k.get(i), i);
				assertDrawn(m, jd, kd);
				assertAmenity(m + " as amenity", BaseDetailsObject.convertRenderedObjectToAmenity(jd, MapPoiTypes.getDefault()),
						net.osmand.shared.data.BaseDetailsObject.Companion.convertRenderedObjectToAmenity(kd,
								net.osmand.shared.osm.MapPoiTypes.Companion.getDefault()));
				for (String lang : LANGS) {
					assertDetails(m + " " + lang, new BaseDetailsObject(jd, lang), new net.osmand.shared.data.BaseDetailsObject(kd, lang),
							List.of(jd), List.of(kd));
				}
				BaseDetailsObject jb = new BaseDetailsObject(j.get(i), "en");
				net.osmand.shared.data.BaseDetailsObject kb = new net.osmand.shared.data.BaseDetailsObject(k.get(i), "en");
				assertEquals(m + " overlaps", jb.overlapsWith(jd), kb.overlapsWith(kd));
				assertEquals(m + " overlaps itself", jb.overlapsWith(j.get(i)), kb.overlapsWith(k.get(i)));
				BaseDetailsObject jother = new BaseDetailsObject(jd, "en");
				net.osmand.shared.data.BaseDetailsObject kother = new net.osmand.shared.data.BaseDetailsObject(kd, "en");
				assertEquals(m + " overlaps a group", jb.overlapsWith(jother), kb.overlapsWith(kother));
				String overlaps = jb.overlapsWith(jd) + "," + jb.overlapsWith(j.get(i)) + "," + jb.overlapsWith(jother);
				assertEquals(m + " added", jb.addObject(jd), kb.addObject(kd));
				assertDetails(m + " with the amenity", jb, kb, List.of(j.get(i), jd), List.of(k.get(i), kd));

				BaseDetailsObject jm = new BaseDetailsObject(j.get(i), "de");
				net.osmand.shared.data.BaseDetailsObject km = new net.osmand.shared.data.BaseDetailsObject(k.get(i), "de");
				jm.merge(jd);
				km.merge(kd);
				jm.merge(jother);
				km.merge(kother);
				assertObjects(m + " merged", jm, km, List.of(j.get(i), jd), List.of(k.get(i), kd));
				assertEquals(m + " merged overlaps", jm.overlapsWith(jother), km.overlapsWith(kother));
				dump.println("R\t" + name(f) + "\t" + i + "\t" + spec(jd) + "\t" + esc(describe(jd))
						+ "\t" + esc(describe(BaseDetailsObject.convertRenderedObjectToAmenity(jd, MapPoiTypes.getDefault())))
						+ "\t" + esc(describe(new BaseDetailsObject(jd, "en"), List.of(jd)))
						+ "\t" + esc(describe(jb, List.of(j.get(i), jd)))
						+ "\t" + overlaps + "\t" + order(jm.getObjects(), List.of(j.get(i), jd)) + "," + jm.overlapsWith(jother));
				drawn++;
			}
		}
		assertTrue("drawn objects: " + drawn, drawn > 1000);
	}

	/** Settlements, streets and houses of each file, next to an amenity of the file. */
	@Test
	public void addressesAreTheSame() throws IOException {
		for (int f = 0; f < files.size(); f++) {
			File file = files.get(f);
			if (javaAmenities.get(f).isEmpty()) {
				continue;
			}
			Amenity ja = javaAmenities.get(f).get(0);
			net.osmand.shared.data.Amenity ka = copyAmenities.get(f).get(0);
			BinaryMapIndexReader java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
			net.osmand.shared.binary.BinaryMapIndexReader copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
			try {
				List<City> jcities = java.getCities(null, CityBlocks.CITY_TOWN_TYPE);
				List<net.osmand.shared.data.City> kcities = copy.getCities(null, net.osmand.shared.binary.CityBlocks.CITY_TOWN_TYPE);
				assertEquals(file.getName() + " settlements", jcities.size(), kcities.size());
				for (int c = 0; c < jcities.size() && c < 5; c++) {
					String line = "A\t" + name(f) + "\t" + c;
					City jc = jcities.get(c);
					net.osmand.shared.data.City kc = kcities.get(c);
					java.preloadStreets(jc, null, null);
					copy.preloadStreets(kc, null);
					List<MapObject> jlist = new ArrayList<>(List.of(jc, ja));
					List<net.osmand.shared.data.MapObject> klist = new ArrayList<>(List.of(kc, ka));
					if (!jc.getStreets().isEmpty()) {
						Street js = jc.getStreets().get(0);
						net.osmand.shared.data.Street ks = kc.getStreets().get(0);
						java.preloadBuildings(js, null, null);
						copy.preloadBuildings(ks, null);
						String m = file.getName() + " " + js;
						BaseDetailsObject jstreet = new BaseDetailsObject(List.of(js, ja), "en");
						assertDetails(m, jstreet, new net.osmand.shared.data.BaseDetailsObject(List.of(ks, ka), "en"), List.of(js, ja), List.of(ks, ka));
						line += "\t" + esc(describe(jstreet, List.of(js, ja)));
						jlist.add(0, js);
						klist.add(0, ks);
						if (!js.getBuildings().isEmpty()) {
							Building jh = js.getBuildings().get(0);
							net.osmand.shared.data.Building kh = ks.getBuildings().get(0);
							BaseDetailsObject jhouse = new BaseDetailsObject(jh, "en");
							assertDetails(m + " " + jh, jhouse, new net.osmand.shared.data.BaseDetailsObject(kh, "en"), List.of(jh), List.of(kh));
							line += "\t" + esc(describe(jhouse, List.of(jh)));
							jlist.add(jh);
							klist.add(kh);
						} else {
							line += "\t-";
						}
					} else {
						line += "\t-\t-";
					}
					String m = file.getName() + " " + jc;
					BaseDetailsObject jall = new BaseDetailsObject(jlist, "en");
					assertDetails(m, jall, new net.osmand.shared.data.BaseDetailsObject(klist, "en"),
							new ArrayList<>(jlist), new ArrayList<>(klist));
					BaseDetailsObject jalone = new BaseDetailsObject(List.of(jc), "de");
					assertDetails(m + " alone", jalone, new net.osmand.shared.data.BaseDetailsObject(List.of(kc), "de"), List.of(jc), List.of(kc));
					dump.println(line + "\t" + esc(describe(jall, jlist)) + "\t" + esc(describe(jalone, List.of(jc))));
					addresses++;
				}
			} finally {
				java.close();
				copy.close();
			}
		}
		assertTrue("addresses: " + addresses, addresses > 50);
	}

	private static String name(int f) {
		return SearchPhraseCompatTest.hex(files.get(f).getName());
	}

	/** What {@code BaseDetailsObjectTest} compares, with doubles as their bits after an {@code @}. */
	static String describe(BaseDetailsObject b, List<?> in) {
		return (b.isObjectFull() ? "F" : b.isObjectCombined() ? "C" : b.isObjectEmpty() ? "E" : "?")
				+ "|" + b.getResourceType().name() + "|" + b.getLang() + "|" + BaseDetailsObject.getLangForTravel(b)
				+ "|" + b.hasGeometry() + "|" + b.getPointsLength() + "|" + b.getAmenities().size()
				+ "|" + b.getRenderedObjects().size() + "|" + index(in, b.getAddressObject())
				+ "|" + order(b.getObjects(), in) + "|" + b + "|" + location(b.getLocation())
				+ "|" + describe(b.getSyntheticAmenity());
	}

	static String describe(Amenity a) {
		StringBuilder s = new StringBuilder();
		s.append(a.getId()).append('|').append(a.getType() == null ? null : a.getType().getKeyName())
				.append('|').append(a.getSubType()).append('|').append(a.getName()).append('|').append(a.getEnName(false))
				.append('|').append(a.getNamesMap(true)).append('|').append(location(a.getLocation())).append('|');
		for (String key : a.getAdditionalInfoKeys()) {
			s.append(key).append('=').append(a.getAdditionalInfo(key)).append(';');
		}
		s.append('|').append(tagGroups(a)).append('|').append(a.getMapIconName()).append('|').append(a.getRegionName())
				.append('|').append(a.getTravelEloNumber()).append('|').append(Arrays.toString(a.getX().toArray()))
				.append('|').append(Arrays.toString(a.getY().toArray())).append('|')
				.append(new TreeSet<>(a.getSupportedContentLocales())).append('|').append(bbox(a.getBbox31()))
				.append('|').append(a);
		return s.toString();
	}

	static String describe(RenderedObject d) {
		QuadRect r = d.getRectLatLon();
		StringBuilder polygon = new StringBuilder();
		for (LatLon l : d.getPolygon()) {
			polygon.append(location(l)).append(';');
		}
		return d + "|" + d.toStringEn() + "|" + ObfConstants.getPrintTags(d) + "|" + d.getOriginalNames()
				+ "|" + d.getRouteID() + "|" + d.isText() + "|" + d.isSimplePoint() + "|" + location(d.getLatLon())
				+ "|" + (r == null ? "null" : "@" + bits(r.left) + ",@" + bits(r.top) + ",@" + bits(r.right) + ",@" + bits(r.bottom))
				+ "|" + polygon;
	}

	/** A drawn object as {@code BaseDetailsObjectTest} makes it again: id, name, place, tags, points, label. */
	static String spec(RenderedObject d) {
		StringBuilder tags = new StringBuilder();
		for (Map.Entry<String, String> e : d.getTags().entrySet()) {
			tags.append(tags.length() > 0 ? ";" : "").append(SearchPhraseCompatTest.hex(e.getKey()))
					.append('=').append(SearchPhraseCompatTest.hex(e.getValue()));
		}
		StringBuilder xy = new StringBuilder();
		for (int p = 0; p < d.getX().size(); p++) {
			xy.append(p > 0 ? " " : "").append(d.getX().get(p)).append(' ').append(d.getY().get(p));
		}
		return (d.getId() == null ? "-" : d.getId().toString()) + "," + SearchPhraseCompatTest.hex(d.getName())
				+ "," + raw(d.getLocation()) + "," + tags + "," + xy + "," + d.getLabelX() + "," + d.getLabelY()
				+ "," + raw(d.getLabelLatLon());
	}

	private static List<Integer> order(List<?> objects, List<?> in) {
		List<Integer> order = new ArrayList<>();
		for (Object o : objects) {
			order.add(index(in, o));
		}
		return order;
	}

	/** A box in 31 tiles, each number after a {@code #}. */
	private static String bbox(int[] bbox) {
		if (bbox == null) {
			return "null";
		}
		StringBuilder s = new StringBuilder();
		for (int v : bbox) {
			s.append(s.length() > 0 ? "," : "").append('#').append(v);
		}
		return s.toString();
	}

	private static String location(LatLon l) {
		return l == null ? "null" : "@" + bits(l.getLatitude()) + ",@" + bits(l.getLongitude());
	}

	private static String raw(LatLon l) {
		return l == null ? "-" : bits(l.getLatitude()) + " " + bits(l.getLongitude());
	}

	private static String bits(double v) {
		return Long.toHexString(Double.doubleToRawLongBits(v));
	}

	private static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static void assertGroup(String m, List<Amenity> j, List<net.osmand.shared.data.Amenity> k, String resource,
			String dumpLine, String dumpLang) {
		for (String lang : LANGS) {
			BaseDetailsObject jb = new BaseDetailsObject(j.get(0), lang);
			net.osmand.shared.data.BaseDetailsObject kb = new net.osmand.shared.data.BaseDetailsObject(k.get(0), lang);
			if (resource != null) {
				jb.setObfResourceName(resource);
				kb.setObfResourceName(resource);
			}
			for (int i = 1; i < j.size(); i++) {
				assertEquals(m + " overlaps " + i, jb.overlapsWith(j.get(i)), kb.overlapsWith(k.get(i)));
				assertEquals(m + " added " + i, jb.addObject(j.get(i)), kb.addObject(k.get(i)));
			}
			assertDetails(m + " added " + lang, jb, kb, j, k);
			BaseDetailsObject jlisted = new BaseDetailsObject(j, lang);
			assertDetails(m + " listed " + lang, jlisted, new net.osmand.shared.data.BaseDetailsObject(k, lang), j, k);
			BaseDetailsObject jnested = new BaseDetailsObject(jb, lang);
			net.osmand.shared.data.BaseDetailsObject knested = new net.osmand.shared.data.BaseDetailsObject(kb, lang);
			assertDetails(m + " nested " + lang, jnested, knested, j, k);
			if (dumpLine != null && Objects.equals(lang, dumpLang)) {
				dump.println(dumpLine + "\t" + esc(describe(jb, j)) + "\t" + esc(describe(jlisted, j)) + "\t" + esc(describe(jnested, j)));
			}
		}
	}

	private static void assertDetails(String m, BaseDetailsObject j, net.osmand.shared.data.BaseDetailsObject k,
			List<?> jin, List<?> kin) {
		assertObjects(m, j, k, jin, kin);
		assertEquals(m + " full", j.isObjectFull(), k.isObjectFull());
		assertEquals(m + " combined", j.isObjectCombined(), k.isObjectCombined());
		assertEquals(m + " empty", j.isObjectEmpty(), k.isObjectEmpty());
		assertEquals(m + " resource", j.getResourceType().name(), k.getResourceType().name());
		assertEquals(m + " lang", j.getLang(), k.getLang());
		assertEquals(m + " lang for travel", BaseDetailsObject.getLangForTravel(j),
				net.osmand.shared.data.BaseDetailsObject.Companion.getLangForTravel(k));
		assertEquals(m + " geometry", j.hasGeometry(), k.hasGeometry());
		assertEquals(m + " points", j.getPointsLength(), k.getPointsLength());
		assertEquals(m + " amenities", j.getAmenities().size(), k.getAmenities().size());
		assertEquals(m + " drawn", j.getRenderedObjects().size(), k.getRenderedObjects().size());
		assertEquals(m + " address", index(jin, j.getAddressObject()), index(kin, k.getAddressObject()));
		assertEquals(m + " toString", j.toString(), k.toString());
		assertLocation(m + " location", j.getLocation(), k.getLocation());
		assertAmenity(m, j.getSyntheticAmenity(), k.getSyntheticAmenity());
		compared++;
	}

	/** The objects each keeps, by where they were in the input. */
	private static void assertObjects(String m, BaseDetailsObject j, net.osmand.shared.data.BaseDetailsObject k,
			List<?> jin, List<?> kin) {
		assertEquals(m + " objects", order(j.getObjects(), jin), order(k.getObjects(), kin));
	}

	private static int index(List<?> list, Object o) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == o) {
				return i;
			}
		}
		return o == null ? -1 : -2;
	}

	private static void assertAmenity(String m, Amenity j, net.osmand.shared.data.Amenity k) {
		assertEquals(m + " id", j.getId(), k.getId());
		assertEquals(m + " type", j.getType() == null ? null : j.getType().getKeyName(),
				k.getType() == null ? null : k.getType().getKeyName());
		assertEquals(m + " subType", j.getSubType(), k.getSubType());
		assertEquals(m + " name", j.getName(), k.getName());
		assertEquals(m + " enName", j.getEnName(false), k.getEnName(false));
		assertEquals(m + " names", j.getNamesMap(true), k.getNamesMap(true));
		assertLocation(m + " location", j.getLocation(), k.getLocation());
		assertEquals(m + " additional keys", new ArrayList<>(j.getAdditionalInfoKeys()),
				new ArrayList<>(k.getAdditionalInfoKeys()));
		for (String key : j.getAdditionalInfoKeys()) {
			assertEquals(m + " additional " + key, j.getAdditionalInfo(key), k.getAdditionalInfo(key));
		}
		assertEquals(m + " tag groups", tagGroups(j), copyTagGroups(k));
		assertEquals(m + " icon", j.getMapIconName(), k.getMapIconName());
		assertEquals(m + " region", j.getRegionName(), k.getRegionName());
		assertEquals(m + " elo", j.getTravelEloNumber(), k.getTravelEloNumber());
		assertArrayEquals(m + " x", j.getX().toArray(), k.getX().toArray());
		assertArrayEquals(m + " y", j.getY().toArray(), k.getY().toArray());
		assertEquals(m + " content locales", new TreeSet<>(j.getSupportedContentLocales()),
				new TreeSet<>(k.getSupportedContentLocales()));
		assertEquals(m + " bbox", Arrays.toString(j.getBbox31()), Arrays.toString(k.getBbox31()));
		assertEquals(m + " toString", j.toString(), k.toString());
	}

	private static void assertLocation(String m, LatLon j, net.osmand.shared.data.KLatLon k) {
		assertEquals(m, j == null, k == null);
		if (j != null) {
			assertEquals(m + " lat", j.getLatitude(), k.getLatitude(), 0);
			assertEquals(m + " lon", j.getLongitude(), k.getLongitude(), 0);
		}
	}

	private static void assertDrawn(String m, RenderedObject j, net.osmand.shared.data.RenderedObject k) {
		assertEquals(m + " toString", j.toString(), k.toString());
		assertEquals(m + " toStringEn", j.toStringEn(), k.toStringEn());
		assertEquals(m + " print tags", ObfConstants.getPrintTags(j), net.osmand.shared.binary.ObfConstants.INSTANCE.getPrintTags(k));
		assertEquals(m + " original names", j.getOriginalNames(), k.getOriginalNames());
		assertEquals(m + " route id", j.getRouteID(), k.getRouteID());
		assertEquals(m + " text", j.isText(), k.isText());
		assertEquals(m + " point", j.isSimplePoint(), k.isSimplePoint());
		assertLocation(m + " lat lon", j.getLatLon(), k.getLatLon());
		QuadRect jr = j.getRectLatLon();
		net.osmand.shared.data.KQuadRect kr = k.getRectLatLon();
		assertEquals(m + " rect", jr == null, kr == null);
		if (jr != null) {
			assertEquals(m + " rect", jr.left + " " + jr.top + " " + jr.right + " " + jr.bottom,
					kr.getLeft() + " " + kr.getTop() + " " + kr.getRight() + " " + kr.getBottom());
		}
		assertEquals(m + " polygon", j.getPolygon().size(), k.getPolygon().size());
		for (int i = 0; i < j.getPolygon().size(); i++) {
			assertLocation(m + " polygon " + i, j.getPolygon().get(i), k.getPolygon().get(i));
		}
	}

	private static RenderedObject drawnJava(Amenity a, int i) {
		RenderedObject d = new RenderedObject();
		PoiType type = MapPoiTypes.getDefault().getPoiTypeByKey(a.getSubType() == null ? "" : a.getSubType());
		Map<String, String> tags = drawnTags(a.getName(), a.getNamesMap(false), a.getSubType(), a.getWikidata(),
				additional(a), i, type == null ? null : new String[] {type.getOsmTag(), type.getOsmValue()});
		for (Map.Entry<String, String> e : tags.entrySet()) {
			d.putTag(e.getKey(), e.getValue());
		}
		d.setName(a.getName());
		d.setId(drawnId(a.getId(), i));
		if (i % 4 != 0) {
			d.setLocation(a.getLocation());
		}
		int[] xy = drawnXY(a.getX().toArray(), a.getY().toArray(), a.getLocation());
		for (int p = 0; p + 1 < xy.length; p += 2) {
			d.addLocation(xy[p], xy[p + 1]);
		}
		if (i % 5 == 1) {
			d.setLabelX(xy[0]);
			d.setLabelY(xy[1]);
		}
		if (i % 7 == 2) {
			d.setLabelLatLon(a.getLocation());
		}
		return d;
	}

	private static net.osmand.shared.data.RenderedObject drawnCopy(net.osmand.shared.data.Amenity a, int i) {
		net.osmand.shared.data.RenderedObject d = new net.osmand.shared.data.RenderedObject();
		net.osmand.shared.osm.PoiType type = net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getPoiTypeByKey(
				a.getSubType() == null ? "" : a.getSubType());
		Map<String, String> tags = drawnTags(a.getName(), a.getNamesMap(false), a.getSubType(), a.getWikidata(),
				copyAdditional(a), i, type == null ? null : new String[] {type.getOsmTag(), type.getOsmValue()});
		for (Map.Entry<String, String> e : tags.entrySet()) {
			d.putTag(e.getKey(), e.getValue());
		}
		d.setName(a.getName());
		d.setId(drawnId(a.getId(), i));
		if (i % 4 != 0) {
			d.setLocation(a.getLocation());
		}
		net.osmand.shared.data.KLatLon l = a.getLocation();
		int[] xy = drawnXY(a.getX().toArray(), a.getY().toArray(), l == null ? null : new LatLon(l.getLatitude(), l.getLongitude()));
		for (int p = 0; p + 1 < xy.length; p += 2) {
			d.addLocation(xy[p], xy[p + 1]);
		}
		if (i % 5 == 1) {
			d.setLabelX(xy[0]);
			d.setLabelY(xy[1]);
		}
		if (i % 7 == 2) {
			d.setLabelLatLon(a.getLocation());
		}
		return d;
	}

	/** Tags as a renderer would keep them, in shapes that take each branch of the conversion. */
	private static Map<String, String> drawnTags(String name, Map<String, String> names, String subType, String wikidata,
			Map<String, String> additional, int i, String[] typeTag) {
		Map<String, String> tags = new LinkedHashMap<>();
		if (!Algorithms.isEmpty(name)) {
			tags.put("name", name);
		}
		if (names != null) {
			for (Map.Entry<String, String> e : names.entrySet()) {
				tags.put("name:" + e.getKey(), e.getValue());
			}
		}
		switch (i % 3) {
			case 0 -> {
				if (typeTag != null && typeTag[0] != null) {
					tags.put(typeTag[0], typeTag[1] == null ? "" : typeTag[1]);
				}
			}
			case 1 -> {
				if (subType != null) {
					tags.put("amenity", subType);
				}
				tags.put("amenity", "cafe");
			}
			default -> {
				if (subType != null) {
					tags.put(subType, "");
				}
			}
		}
		int n = 0;
		for (Map.Entry<String, String> e : additional.entrySet()) {
			if (n++ < 4) {
				tags.put(e.getKey(), e.getValue());
			}
		}
		if (wikidata != null) {
			tags.put(Amenity.WIKIDATA, wikidata);
		}
		if (i % 11 == 0) {
			tags.put(Amenity.ROUTE_ID, "O" + i);
			tags.put("", "");
		}
		return tags;
	}

	/** The amenity's id packed the way the renderer packs ids, or a relation or a propagated node. */
	private static Long drawnId(Long id, int i) {
		if (id == null) {
			return null;
		}
		long osm = ObfConstants.getOsmIdFromMapObjectId(id);
		return switch (i % 4) {
			case 0 -> osm << ObfConstants.SHIFT_ID;
			case 1 -> (ObfConstants.RELATION_BIT + ((osm << ObfConstants.SHIFT_ID) << ObfConstants.DUPLICATE_SPLIT)) << 1;
			case 2 -> (ObfConstants.PROPAGATE_NODE_BIT + (osm << ObfConstants.SHIFT_PROPAGATED_NODES_BITS)) << 1;
			default -> id;
		};
	}

	private static int[] drawnXY(int[] x, int[] y, LatLon location) {
		if (x.length > 0 && x.length == y.length) {
			int[] xy = new int[x.length * 2];
			for (int p = 0; p < x.length; p++) {
				xy[p * 2] = x[p];
				xy[p * 2 + 1] = y[p];
			}
			return xy;
		}
		return new int[] {MapUtils.get31TileNumberX(location.getLongitude()), MapUtils.get31TileNumberY(location.getLatitude())};
	}

	private static Map<String, String> additional(Amenity a) {
		Map<String, String> m = new LinkedHashMap<>();
		for (String key : a.getAdditionalInfoKeys()) {
			m.put(key, a.getAdditionalInfo(key));
		}
		return m;
	}

	private static Map<String, String> copyAdditional(net.osmand.shared.data.Amenity a) {
		Map<String, String> m = new LinkedHashMap<>();
		for (String key : a.getAdditionalInfoKeys()) {
			m.put(key, a.getAdditionalInfo(key));
		}
		return m;
	}

	/** A copy to change the region and the tags of, leaving the amenity read from the file as it is. */
	private static Amenity copyOf(Amenity a) {
		Amenity c = new Amenity();
		c.setId(a.getId());
		c.setType(a.getType());
		c.setSubType(a.getSubType());
		c.setLocation(a.getLocation());
		c.setRegionName(a.getRegionName());
		c.copyNames(a);
		c.copyAdditionalInfo(a, false);
		c.setTagGroups(a.getTagGroups() == null ? null : new HashMap<>(a.getTagGroups()));
		c.setX(a.getX());
		c.setY(a.getY());
		return c;
	}

	private static net.osmand.shared.data.Amenity copyOf(net.osmand.shared.data.Amenity a) {
		net.osmand.shared.data.Amenity c = new net.osmand.shared.data.Amenity();
		c.setId(a.getId());
		c.setType(a.getType());
		c.setSubType(a.getSubType());
		c.setLocation(a.getLocation());
		c.setRegionName(a.getRegionName());
		c.copyNames(a);
		c.copyAdditionalInfo(a, false);
		c.setTagGroups(a.getTagGroups() == null ? null : new HashMap<>(a.getTagGroups()));
		c.setX(a.getX());
		c.setY(a.getY());
		return c;
	}

	private static Map<Integer, String> tagGroups(Amenity a) {
		Map<Integer, String> groups = new java.util.TreeMap<>();
		if (a.getTagGroups() != null) {
			for (Map.Entry<Integer, List<TagValuePair>> e : a.getTagGroups().entrySet()) {
				StringBuilder s = new StringBuilder();
				for (TagValuePair p : e.getValue()) {
					s.append(p.tag).append('=').append(p.value).append(';');
				}
				groups.put(e.getKey(), s.toString());
			}
		}
		return groups;
	}

	private static Map<Integer, String> copyTagGroups(net.osmand.shared.data.Amenity a) {
		Map<Integer, String> groups = new java.util.TreeMap<>();
		if (a.getTagGroups() != null) {
			for (Map.Entry<Integer, List<net.osmand.shared.binary.TagValuePair>> e : a.getTagGroups().entrySet()) {
				StringBuilder s = new StringBuilder();
				for (net.osmand.shared.binary.TagValuePair p : e.getValue()) {
					s.append(p.tag).append('=').append(p.value).append(';');
				}
				groups.put(e.getKey(), s.toString());
			}
		}
		return groups;
	}
}
