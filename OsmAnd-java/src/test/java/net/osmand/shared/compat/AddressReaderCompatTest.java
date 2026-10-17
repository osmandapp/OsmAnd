package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CitiesBlock;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.shared.api.KStringMatcherMode;
import net.osmand.shared.data.KLatLon;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * The address section: {@link net.osmand.shared.binary.BinaryMapIndexReader} against
 * {@link BinaryMapIndexReader}, asked the same questions about the same files.
 *
 * The files are the routing test maps and the maps of the search tests, which are small extracts
 * of real regions with the address section each search case needs. Every settlement of every block
 * type is read on both sides, then every street of it, then every house and crossing of every
 * street, and each object is compared field by field; the streets are read a second time with
 * their houses in one pass, which is a different path through the reader. The name index is then
 * asked for the names the file actually holds, whole and cut short, in every matcher mode, by block
 * type and inside a box, and the results are compared in order, together with what the raw data
 * collector was handed.
 *
 * <b>The query is lowercased for java.</b> Both callers of the name search hand it a lowercased
 * word: {@code SearchCoreFactory} lowercases it, {@code GeocodingUtilities} takes it from
 * {@code splitAndNormalize}. Java does not lowercase it itself where it walks the string table of
 * the index, with the static {@code CollatorStringMatcher.cmatches}, and its collator does not fold
 * the case of most scripts, so "Кра" does not reach the key "кра" there; the copy lowercases on
 * every path, as {@code CollatorCompatTest} describes. {@link #copyFoldsCaseItself} holds the copy
 * with the query as typed against java with it lowercased.
 */
@RunWith(Parameterized.class)
public class AddressReaderCompatTest {

	private static final int NAMES_PER_FILE = 30;

	private static int filesWithAddress;
	private static int citiesCompared;
	private static int streetsCompared;
	private static int buildingsCompared;
	private static int searchesCompared;
	private static int objectsFound;

	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	public AddressReaderCompatTest(String name, File file) throws IOException {
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@AfterClass
	public static void addressesWereCompared() {
		System.out.println("AddressReaderCompatTest: " + filesWithAddress + " files with an address section, "
				+ citiesCompared + " settlements, " + streetsCompared + " streets, " + buildingsCompared
				+ " houses compared; " + searchesCompared + " searches by name, " + objectsFound + " objects found");
		assertTrue("files with an address section: " + filesWithAddress, filesWithAddress > 10);
		assertTrue("settlements compared: " + citiesCompared, citiesCompared > 1000);
		assertTrue("streets compared: " + streetsCompared, streetsCompared > 1000);
		assertTrue("houses compared: " + buildingsCompared, buildingsCompared > 1000);
		assertTrue("searches compared: " + searchesCompared, searchesCompared > 1000);
		assertTrue("objects found: " + objectsFound, objectsFound > 1000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<Object[]> data = new ArrayList<>();
		List<File> files = new ArrayList<>(TestObf.files());
		files.addAll(TestObf.searchFiles());
		for (File file : files) {
			data.add(new Object[] {file.getName(), file});
		}
		return data;
	}

	/** The sections as the header walk found them, and what the reader says about the region. */
	@Test
	public void regionsAreTheSame() {
		List<AddressRegion> jregions = java.getAddressIndexes();
		List<net.osmand.shared.binary.AddressRegion> kregions = copy.getAddressIndexes();
		assertEquals("sections", jregions.size(), kregions.size());
		for (int i = 0; i < jregions.size(); i++) {
			AddressRegion j = jregions.get(i);
			net.osmand.shared.binary.AddressRegion k = kregions.get(i);
			String m = "section " + i;
			assertEquals(m + " name", j.getName(), k.getName());
			assertEquals(m + " enName", j.getEnName(), k.getEnName());
			assertEquals(m + " filePointer", j.getFilePointer(), k.getFilePointer());
			assertEquals(m + " length", j.getLength(), k.getLength());
			assertEquals(m + " indexNameOffset", j.getIndexNameOffset(), k.getIndexNameOffset());
			assertEquals(m + " attributeTagsTable", j.getAttributeTagsTable(), k.getAttributeTagsTable());
			assertEquals(m + " blocks", j.getCities().size(), k.getCities().size());
			for (int b = 0; b < j.getCities().size(); b++) {
				CitiesBlock jb = j.getCities().get(b);
				net.osmand.shared.binary.CitiesBlock kb = k.getCities().get(b);
				assertEquals(m + " block " + b + " type", jb.getType(), kb.getType());
				assertEquals(m + " block " + b + " filePointer", jb.getFilePointer(), kb.getFilePointer());
				assertEquals(m + " block " + b + " length", jb.getLength(), kb.getLength());
			}
		}
		assertEquals("containsAddressData", java.containsAddressData(), copy.containsAddressData());
		assertEquals("hasRegions", java.hasRegions(), copy.hasRegions());
		assertEquals("regionNames", java.getRegionNames(), copy.getRegionNames());
		assertEquals("countryName", java.getCountryName(), copy.getCountryName());
		assertEquals("regionName", java.getRegionName(), copy.getRegionName());
		assertEquals("regionCenter", describe(java.getRegionCenter()), describe(copy.getRegionCenter()));
		if (!jregions.isEmpty()) {
			filesWithAddress++;
		}
	}

	/**
	 * Every settlement of every block type; its streets; their houses and crossings, read street by
	 * street; the houses in the order the street sorts them, and the boxes worked out from them.
	 */
	@Test
	public void settlementsStreetsAndHousesAreTheSame() throws IOException {
		for (CityBlocks type : CityBlocks.values()) {
			net.osmand.shared.binary.CityBlocks ktype = net.osmand.shared.binary.CityBlocks.valueOf(type.name());
			List<City> jcities = java.getCities(null, type);
			List<net.osmand.shared.data.City> kcities = copy.getCities(null, ktype);
			String m = type + " ";
			assertEquals(m + "settlements", describeJavaCities(jcities), describeCopyCities(kcities));
			for (int i = 0; i < jcities.size(); i++) {
				City jc = jcities.get(i);
				net.osmand.shared.data.City kc = kcities.get(i);
				String cm = m + jc;
				int jsize = java.preloadStreets(jc, null, null);
				int ksize = copy.preloadStreets(kc, null);
				assertEquals(cm + " streets block size", jsize, ksize);
				for (int s = 0; s < jc.getStreets().size(); s++) {
					java.preloadBuildings(jc.getStreets().get(s), null, null);
					copy.preloadBuildings(kc.getStreets().get(s), null);
				}
				assertEquals(cm + " streets", describeJavaStreets(jc.getStreets()), describeCopyStreets(kc.getStreets()));
				for (int s = 0; s < jc.getStreets().size(); s++) {
					Street js = jc.getStreets().get(s);
					net.osmand.shared.data.Street ks = kc.getStreets().get(s);
					assertEquals(cm + " " + js + " bbox", Arrays.toString(js.getBbox31()), Arrays.toString(ks.getBbox31()));
					js.sortBuildings();
					ks.sortBuildings();
					assertEquals(cm + " " + js + " sorted houses", describeJavaBuildings(js.getBuildings()),
							describeCopyBuildings(ks.getBuildings()));
					compareInterpolations(cm + " " + js, js.getBuildings(), ks.getBuildings());
					buildingsCompared += js.getBuildings().size();
				}
				if (!jc.getStreets().isEmpty()) {
					jc.calculateBbox31FromStreets();
					kc.calculateBbox31FromStreets();
					assertEquals(cm + " bbox from streets", Arrays.toString(jc.getBbox31()), Arrays.toString(kc.getBbox31()));
				}
				streetsCompared += jc.getStreets().size();
			}
			citiesCompared += jcities.size();
		}
	}

	/** The streets of every settlement with their houses and crossings read in the same pass. */
	@Test
	public void streetsWithHousesInOnePassAreTheSame() throws IOException {
		for (CityBlocks type : CityBlocks.values()) {
			net.osmand.shared.binary.CityBlocks ktype = net.osmand.shared.binary.CityBlocks.valueOf(type.name());
			List<City> jcities = java.getCities(null, type);
			List<net.osmand.shared.data.City> kcities = copy.getCities(null, ktype);
			assertEquals(type + " settlements", jcities.size(), kcities.size());
			for (int i = 0; i < jcities.size(); i++) {
				City jc = jcities.get(i);
				net.osmand.shared.data.City kc = kcities.get(i);
				java.preloadStreets(jc, null, true, null);
				copy.preloadStreets(kc, null, true);
				assertEquals(type + " " + jc + " streets", describeJavaStreets(jc.getStreets()),
						describeCopyStreets(kc.getStreets()));
			}
		}
	}

	/**
	 * The name index, asked for names the file holds: whole, the first word, the first three
	 * letters. Each in every mode over everything; the whole names also by block type and inside a
	 * box around the region's centre.
	 */
	@Test
	public void searchByNameIsTheSame() throws IOException {
		LatLon center = java.getRegionCenter();
		for (String typed : queries()) {
			String query = typed.toLowerCase();
			for (StringMatcherMode mode : StringMatcherMode.values()) {
				compareSearch("'" + query + "' " + mode, query, query, mode, null, null);
			}
			for (CityBlocks type : CityBlocks.allTypes()) {
				compareSearch("'" + query + "' " + type, query, query, StringMatcherMode.CHECK_STARTS_FROM_SPACE,
						Collections.singletonList(type), null);
			}
			if (center != null) {
				compareSearch("'" + query + "' near the centre", query, query,
						StringMatcherMode.CHECK_STARTS_FROM_SPACE, null, center);
			}
		}
	}

	/** The copy asked with the query as typed finds what java finds with it lowercased. */
	@Test
	public void copyFoldsCaseItself() throws IOException {
		for (String typed : queries()) {
			String query = typed.toLowerCase();
			if (query.equals(typed)) {
				continue;
			}
			for (StringMatcherMode mode : new StringMatcherMode[] {StringMatcherMode.CHECK_ONLY_STARTS_WITH,
					StringMatcherMode.CHECK_STARTS_FROM_SPACE, StringMatcherMode.CHECK_EQUALS_FROM_SPACE}) {
				compareSearch("'" + typed + "' " + mode, query, typed, mode, null, null);
			}
		}
	}

	private void compareSearch(String m, String javaQuery, String copyQuery, StringMatcherMode mode,
							   List<CityBlocks> types, LatLon near) throws IOException {
		List<MapObject> jraw = new ArrayList<>();
		SearchRequest<MapObject> jreq = BinaryMapIndexReader.buildAddressByNameRequest(null,
				new ResultMatcher<MapObject>() {
					@Override
					public boolean publish(MapObject object) {
						jraw.add(object);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}, javaQuery, mode);
		List<net.osmand.shared.data.MapObject> kraw = new ArrayList<>();
		net.osmand.shared.binary.SearchRequest<net.osmand.shared.data.MapObject> kreq =
				net.osmand.shared.binary.SearchRequest.buildAddressByNameRequest(null,
						new net.osmand.shared.binary.ResultMatcher<net.osmand.shared.data.MapObject>() {
							@Override
							public boolean publish(net.osmand.shared.data.MapObject object) {
								kraw.add(object);
								return true;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						}, copyQuery, KStringMatcherMode.valueOf(mode.name()));
		if (near != null) {
			jreq.setBBoxRadius(near.getLatitude(), near.getLongitude(), 20000);
			kreq.setBBoxRadius(near.getLatitude(), near.getLongitude(), 20000);
		}
		List<net.osmand.shared.binary.CityBlocks> ktypes = null;
		if (types != null) {
			ktypes = new ArrayList<>();
			for (CityBlocks type : types) {
				ktypes.add(net.osmand.shared.binary.CityBlocks.valueOf(type.name()));
			}
		}
		List<MapObject> jres = java.searchAddressDataByName(jreq, types);
		List<net.osmand.shared.data.MapObject> kres = copy.searchAddressDataByName(kreq, ktypes);
		assertEquals(m + " found", describeJavaObjects(jres), describeCopyObjects(kres));
		assertEquals(m + " raw data", describeJavaObjects(jraw), describeCopyObjects(kraw));
		searchesCompared++;
		objectsFound += jres.size();
	}

	/** Names of the settlements, postcodes and streets of the file, spread over it, and parts of them. */
	private List<String> queries() throws IOException {
		List<String> names = new ArrayList<>();
		for (CityBlocks type : CityBlocks.allTypes()) {
			for (City c : java.getCities(null, type)) {
				names.add(c.getName());
				if (names.size() < NAMES_PER_FILE) {
					java.preloadStreets(c, null, null);
					for (Street s : c.getStreets()) {
						names.add(s.getName());
					}
				}
			}
		}
		Set<String> queries = new LinkedHashSet<>();
		int step = Math.max(1, names.size() / NAMES_PER_FILE);
		for (int i = 0; i < names.size() && queries.size() < 3 * NAMES_PER_FILE; i += step) {
			String name = names.get(i).trim();
			if (name.isEmpty()) {
				continue;
			}
			queries.add(name);
			int space = name.indexOf(' ');
			if (space > 0) {
				queries.add(name.substring(0, space));
			}
			if (name.length() > 3) {
				queries.add(name.substring(0, 3));
			}
		}
		return new ArrayList<>(queries);
	}

	private static void compareInterpolations(String m, List<Building> jbuildings,
											  List<net.osmand.shared.data.Building> kbuildings) {
		for (int i = 0; i < jbuildings.size(); i++) {
			Building j = jbuildings.get(i);
			net.osmand.shared.data.Building k = kbuildings.get(i);
			if (!j.isInterpolation()) {
				continue;
			}
			String bm = m + " " + j;
			assertEquals(bm + " isInterpolation", j.isInterpolation(), k.isInterpolation());
			for (double coeff : new double[] {0, 0.25, 0.5, 1}) {
				assertEquals(bm + " interpolationName " + coeff, j.getInterpolationName(coeff), k.getInterpolationName(coeff));
			}
			List<String> numbers = new ArrayList<>();
			numbers.add(j.getName());
			if (j.getName2() != null) {
				numbers.add(j.getName2());
			}
			numbers.add(j.getInterpolationName(0.5));
			for (int n = 0; n < 20; n++) {
				numbers.add(String.valueOf(n));
			}
			for (String hno : numbers) {
				if (hno.isEmpty()) {
					continue;
				}
				Same.outcome(bm + " interpolation " + hno, () -> j.interpolation(hno), () -> k.interpolation(hno));
				Same.outcome(bm + " belongs " + hno, () -> j.belongsToInterpolation(hno), () -> k.belongsToInterpolation(hno));
				Same.outcome(bm + " location " + hno, () -> describe(j.getLocation(j.interpolation(hno))),
						() -> describe(k.getLocation(k.interpolation(hno))));
			}
		}
	}

	private static List<String> describeJavaCities(List<City> cities) {
		List<String> res = new ArrayList<>();
		for (City c : cities) {
			res.add(describe(c));
		}
		return res;
	}

	private static List<String> describeCopyCities(List<net.osmand.shared.data.City> cities) {
		List<String> res = new ArrayList<>();
		for (net.osmand.shared.data.City c : cities) {
			res.add(describe(c));
		}
		return res;
	}

	private static List<String> describeJavaStreets(List<Street> streets) {
		List<String> res = new ArrayList<>();
		for (Street s : streets) {
			res.add(describe(s));
		}
		return res;
	}

	private static List<String> describeCopyStreets(List<net.osmand.shared.data.Street> streets) {
		List<String> res = new ArrayList<>();
		for (net.osmand.shared.data.Street s : streets) {
			res.add(describe(s));
		}
		return res;
	}

	private static List<String> describeJavaBuildings(List<Building> buildings) {
		List<String> res = new ArrayList<>();
		for (Building b : buildings) {
			res.add(describe(b));
		}
		return res;
	}

	private static List<String> describeCopyBuildings(List<net.osmand.shared.data.Building> buildings) {
		List<String> res = new ArrayList<>();
		for (net.osmand.shared.data.Building b : buildings) {
			res.add(describe(b));
		}
		return res;
	}

	private static List<String> describeJavaObjects(List<MapObject> objects) {
		List<String> res = new ArrayList<>();
		for (MapObject o : objects) {
			if (o instanceof City) {
				res.add("City " + describe((City) o));
			} else if (o instanceof Street) {
				res.add("Street " + describe((Street) o));
			} else {
				res.add(String.valueOf(o));
			}
		}
		return res;
	}

	private static List<String> describeCopyObjects(List<net.osmand.shared.data.MapObject> objects) {
		List<String> res = new ArrayList<>();
		for (net.osmand.shared.data.MapObject o : objects) {
			if (o instanceof net.osmand.shared.data.City) {
				res.add("City " + describe((net.osmand.shared.data.City) o));
			} else if (o instanceof net.osmand.shared.data.Street) {
				res.add("Street " + describe((net.osmand.shared.data.Street) o));
			} else {
				res.add(String.valueOf(o));
			}
		}
		return res;
	}

	private static String describe(City c) {
		return names(c) + " type " + c.getType().name() + " postcode " + c.getPostcode() + " isPostcode "
				+ c.isPostcode() + " bbox " + Arrays.toString(c.getBbox31()) + " toString " + c;
	}

	private static String describe(net.osmand.shared.data.City c) {
		return names(c) + " type " + c.getType().name() + " postcode " + c.getPostcode() + " isPostcode "
				+ c.isPostcode() + " bbox " + Arrays.toString(c.getBbox31()) + " toString " + c;
	}

	private static String describe(Street s) {
		City c = s.getCity();
		StringBuilder sb = new StringBuilder(names(s));
		sb.append(" city ").append(c == null ? "null" : c.getId() + " " + c.getName());
		sb.append(" houses ").append(describeJavaBuildings(s.getBuildings()));
		sb.append(" crossings ").append(describeJavaStreets(s.getIntersectedStreets()));
		return sb.toString();
	}

	private static String describe(net.osmand.shared.data.Street s) {
		net.osmand.shared.data.City c = s.getCity();
		StringBuilder sb = new StringBuilder(names(s));
		sb.append(" city ").append(c == null ? "null" : c.getId() + " " + c.getName());
		sb.append(" houses ").append(describeCopyBuildings(s.getBuildings()));
		sb.append(" crossings ").append(describeCopyStreets(s.getIntersectedStreets()));
		return sb.toString();
	}

	private static String describe(Building b) {
		return names(b) + " name2 " + b.getName2() + " location2 " + describe(b.getLatLon2()) + " postcode "
				+ b.getPostcode() + " interpolation " + b.getInterpolationType() + " " + b.getInterpolationInterval()
				+ " fullName " + b.getFullName() + " nameEn " + b.getName("en") + " toString " + b;
	}

	private static String describe(net.osmand.shared.data.Building b) {
		return names(b) + " name2 " + b.getName2() + " location2 " + describe(b.getLatLon2()) + " postcode "
				+ b.getPostcode() + " interpolation " + b.getInterpolationType() + " " + b.getInterpolationInterval()
				+ " fullName " + b.getFullName() + " nameEn " + b.getName("en") + " toString " + b;
	}

	private static String names(MapObject o) {
		return "id " + o.getId() + " name " + o.getName() + " en " + o.getEnName(false) + " names "
				+ new TreeMap<>(o.getNamesMap(true)) + " location " + describe(o.getLocation()) + " offset "
				+ o.getFileOffset();
	}

	private static String names(net.osmand.shared.data.MapObject o) {
		return "id " + o.getId() + " name " + o.getName() + " en " + o.getEnName(false) + " names "
				+ new TreeMap<>(o.getNamesMap(true)) + " location " + describe(o.getLocation()) + " offset "
				+ o.getFileOffset();
	}

	private static String describe(LatLon l) {
		return l == null ? "null" : l.getLatitude() + "," + l.getLongitude();
	}

	private static String describe(KLatLon l) {
		return l == null ? "null" : l.getLatitude() + "," + l.getLongitude();
	}

}
