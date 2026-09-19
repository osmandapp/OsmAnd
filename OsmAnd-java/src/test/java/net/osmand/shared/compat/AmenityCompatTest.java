package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.MapPoiTypes;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * {@link net.osmand.shared.data.Amenity} and {@link net.osmand.shared.data.MapObject} are copies of
 * {@link Amenity} and {@link MapObject}; this builds the same amenities on both sides and asks
 * every tag helper the same questions.
 *
 * The amenities are built rather than read out of a file, because the poi section of the reader is
 * not copied yet - that is the next step, and it will compare what it reads through these same
 * helpers. What is built here is what that section produces: names in several languages, tags with
 * language suffixes, a gzipped description, the tag groups of the settlements around the object,
 * and a track stored as an amenity.
 */
public class AmenityCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes java = new MapPoiTypes(POI_TYPES);
		MapPoiTypes.setDefault(java);
		net.osmand.shared.osm.MapPoiTypes copy = new net.osmand.shared.osm.MapPoiTypes(POI_TYPES);
		net.osmand.shared.osm.MapPoiTypes.setDefault(copy);
	}

	@Test
	public void namesAnswerTheSame() {
		for (Sample sample : samples()) {
			String m = sample.name;
			Amenity j = sample.java();
			net.osmand.shared.data.Amenity k = sample.copy();

			assertEquals(m + " name", j.getName(), k.getName());
			assertEquals(m + " id", j.getId(), k.getId());
			assertEquals(m + " osmId", j.getOsmId(), k.getOsmId());
			assertEquals(m + " enName", j.getEnName(false), k.getEnName(false));
			assertEquals(m + " enName transliterated", j.getEnName(true), k.getEnName(true));
			assertEquals(m + " names map", j.getNamesMap(true), k.getNamesMap(true));
			assertEquals(m + " names map no en", j.getNamesMap(false), k.getNamesMap(false));
			assertEquals(m + " other names", j.getOtherNames(), k.getOtherNames());
			assertEquals(m + " other names transliterated", j.getOtherNames(true), k.getOtherNames(true));
			assertEquals(m + " other names locale", j.getOtherNames(false, "Bäckerei"),
					k.getOtherNames(false, "Bäckerei"));
			for (String lang : new String[] {null, "", "en", "de", "ru", "zz"}) {
				assertEquals(m + " name " + lang, j.getName(lang), k.getName(lang));
				assertEquals(m + " name transliterated " + lang, j.getName(lang, true),
						k.getName(lang, true));
			}
			assertEquals(m + " wikidata", j.getWikidata(), k.getWikidata());
			assertEquals(m + " toString", j.toString(), k.toString());
			assertEquals(m + " toStringEn", j.toStringEn(), k.toStringEn());
			assertEquals(m + " location lat", j.getLocation().getLatitude(),
					k.getLocation().getLatitude(), 0);
			assertEquals(m + " location lon", j.getLocation().getLongitude(),
					k.getLocation().getLongitude(), 0);
			assertEquals(m + " fileOffset", j.getFileOffset(), k.getFileOffset());
		}
	}

	@Test
	public void tagHelpersAnswerTheSame() {
		for (Sample sample : samples()) {
			String m = sample.name;
			Amenity j = sample.java();
			net.osmand.shared.data.Amenity k = sample.copy();

			assertEquals(m + " subType", j.getSubType(), k.getSubType());
			assertEquals(m + " type", j.getType().getKeyName(), k.getType().getKeyName());
			assertEquals(m + " mainSubtype", j.getMainSubtype(), k.getMainSubtype());
			assertEquals(m + " subTypeStr", j.getSubTypeStr(), k.getSubTypeStr());
			assertEquals(m + " icon", j.getIcon(), k.getIcon());
			assertEquals(m + " order", j.getOrder(), k.getOrder());
			assertEquals(m + " regionName", j.getRegionName(), k.getRegionName());
			assertEquals(m + " hasAdditionalInfo", j.hasAdditionalInfo(), k.hasAdditionalInfo());
			assertEquals(m + " additionalInfoKeys", new ArrayList<>(j.getAdditionalInfoKeys()),
					new ArrayList<>(k.getAdditionalInfoKeys()));
			assertEquals(m + " additionalInfoValues", new ArrayList<>(j.getAdditionalInfoValues(false)),
					new ArrayList<>(k.getAdditionalInfoValues(false)));
			assertEquals(m + " additionalInfoValues excl zipped",
					new ArrayList<>(j.getAdditionalInfoValues(true)),
					new ArrayList<>(k.getAdditionalInfoValues(true)));
			for (String key : j.getAdditionalInfoKeys()) {
				assertEquals(m + " additionalInfo " + key, j.getAdditionalInfo(key),
						k.getAdditionalInfo(key));
			}
			assertEquals(m + " openingHours", j.getOpeningHours(), k.getOpeningHours());
			assertEquals(m + " site", j.getSite(), k.getSite());
			assertEquals(m + " phone", j.getPhone(), k.getPhone());
			assertEquals(m + " color", j.getColor(), k.getColor());
			assertEquals(m + " gpxIcon", j.getGpxIcon(), k.getGpxIcon());
			assertEquals(m + " street", j.getStreetName(), k.getStreetName());
			assertEquals(m + " housenumber", j.getHousenumber(), k.getHousenumber());
			assertEquals(m + " ref", j.getRef(), k.getRef());
			assertEquals(m + " routeId", j.getRouteId(), k.getRouteId());
			assertEquals(m + " wikiPhoto", j.getWikiPhoto(), k.getWikiPhoto());
			assertEquals(m + " wikiCategory", j.getWikiCategory(), k.getWikiCategory());
			assertEquals(m + " travelTopic", j.getTravelTopic(), k.getTravelTopic());
			assertEquals(m + " travelElo", j.getTravelElo(), k.getTravelElo());
			assertEquals(m + " travelEloNumber", j.getTravelEloNumber(), k.getTravelEloNumber());
			assertEquals(m + " osmandPoiKey", j.getOsmandPoiKey(), k.getOsmandPoiKey());
			assertEquals(m + " altNames", j.getAltNamesMap(), k.getAltNamesMap());
			assertEquals(m + " closed", j.isClosed(), k.isClosed());
			assertEquals(m + " privateAccess", j.isPrivateAccess(), k.isPrivateAccess());
			assertEquals(m + " routeTrack", j.isRouteTrack(), k.isRouteTrack());
			assertEquals(m + " routePoint", j.isRoutePoint(), k.isRoutePoint());
			assertEquals(m + " routeArticle", j.isRouteArticle(), k.isRouteArticle());
			assertEquals(m + " superRoute", j.isSuperRoute(), k.isSuperRoute());
			assertEquals(m + " hasOsmRouteId", j.hasOsmRouteId(), k.hasOsmRouteId());
			assertEquals(m + " routeActivityType", j.getRouteActivityType(), k.getRouteActivityType());
			assertEquals(m + " supportedContentLocales",
					new ArrayList<>(j.getSupportedContentLocales()),
					new ArrayList<>(k.getSupportedContentLocales()));
			for (String lang : new String[] {null, "en", "de", "uk"}) {
				assertEquals(m + " description " + lang, j.getDescription(lang), k.getDescription(lang));
				assertEquals(m + " tagContent description " + lang,
						j.getTagContent("description", lang), k.getTagContent("description", lang));
				assertEquals(m + " strictTagContent " + lang,
						j.getStrictTagContent("description", lang), k.getStrictTagContent("description", lang));
				assertEquals(m + " contentLanguage " + lang,
						j.getContentLanguage("description", lang, "en"),
						k.getContentLanguage("description", lang, "en"));
				assertEquals(m + " gpxFileName " + lang, j.getGpxFileName(lang), k.getGpxFileName(lang));
				assertEquals(m + " cityFromTagGroups " + lang, j.getCityFromTagGroups(lang),
						k.getCityFromTagGroups(lang));
			}
			assertEquals(m + " tagSuffix", j.getTagSuffix("route_activity_type_"),
					k.getTagSuffix("route_activity_type_"));
			assertEquals(m + " names of content", j.getNames("content", "en"), k.getNames("content", "en"));
			assertEquals(m + " osmTags", j.getOsmTags(), k.getOsmTags());
			assertEquals(m + " polygon size", j.getPolygon().size(), k.getPolygon().size());
			for (int i = 0; i < j.getPolygon().size(); i++) {
				LatLon jl = j.getPolygon().get(i);
				net.osmand.shared.data.KLatLon kl = k.getPolygon().get(i);
				assertEquals(m + " polygon lat " + i, jl.getLatitude(), kl.getLatitude(), 0);
				assertEquals(m + " polygon lon " + i, jl.getLongitude(), kl.getLongitude(), 0);
			}
			assertEquals(m + " bbox31", Arrays.toString(j.getBbox31()), Arrays.toString(k.getBbox31()));
			assertEquals(m + " printNamesAndAdditional", j.printNamesAndAdditional().toString(),
					k.printNamesAndAdditional().toString());
			assertEquals(m + " poiStringWithoutType",
					Amenity.getPoiStringWithoutType(j, "de", false),
					net.osmand.shared.data.Amenity.getPoiStringWithoutType(k, "de", false));
		}
	}

	/** What a track built out of an amenity carries over, and what it deliberately drops. */
	@Test
	public void extensionsAreTheSame() {
		for (Sample sample : samples()) {
			String m = sample.name;
			Amenity j = sample.java();
			net.osmand.shared.data.Amenity k = sample.copy();
			for (boolean prefixes : new boolean[] {true, false}) {
				assertEquals(m + " extensions prefixes " + prefixes,
						j.getAmenityExtensions(MapPoiTypes.getDefault(), prefixes),
						k.getAmenityExtensions(net.osmand.shared.osm.MapPoiTypes.getDefault(), prefixes));
				for (String lang : new String[] {null, "en", "de"}) {
					assertEquals(m + " extensions no wiki " + prefixes + " " + lang,
							j.getAmenityExtensions(MapPoiTypes.getDefault(), prefixes, true, lang),
							k.getAmenityExtensions(net.osmand.shared.osm.MapPoiTypes.getDefault(),
									prefixes, true, lang));
				}
			}
			Map<String, String> extensions = j.getAmenityExtensions();
			for (String lang : new String[] {null, "en", "de"}) {
				assertEquals(m + " removeWikiContentTags " + lang,
						Amenity.removeWikiContentTags(extensions, lang),
						net.osmand.shared.data.Amenity.removeWikiContentTags(extensions, lang));
			}
			for (String key : j.getAdditionalInfoKeys()) {
				for (String lang : new String[] {null, "en", "de"}) {
					assertEquals(m + " isWikiContentTag " + key + " " + lang,
							Amenity.isWikiContentTag(key, lang, true),
							net.osmand.shared.data.Amenity.isWikiContentTag(key, lang, true));
					assertEquals(m + " isWikiContentTag no default " + key + " " + lang,
							Amenity.isWikiContentTag(key, lang, false),
							net.osmand.shared.data.Amenity.isWikiContentTag(key, lang, false));
				}
			}
		}
	}

	/** Comparing amenities against each other, which is how duplicates across files are dropped. */
	@Test
	public void comparisonsAreTheSame() {
		List<Sample> samples = samples();
		for (Sample a : samples) {
			for (Sample b : samples) {
				String m = a.name + " vs " + b.name;
				assertEquals(m + " equals", a.java().equals(b.java()), a.copy().equals(b.copy()));
				assertEquals(m + " comparePoi", a.java().comparePoi(b.java()),
						a.copy().comparePoi(b.copy()));
				assertEquals(m + " compareObject", a.java().compareObject(b.java()),
						a.copy().compareObject(b.copy()));
				assertEquals(m + " strictEquals", a.java().strictEquals(b.java()),
						a.copy().strictEquals(b.copy()));
				assertEquals(m + " compareTo", Integer.signum(a.java().compareTo(b.java())),
						Integer.signum(a.copy().compareTo(b.copy())));
				assertEquals(m + " byName", Integer.signum(MapObject.BY_NAME_COMPARATOR.compare(a.java(), b.java())),
						Integer.signum(net.osmand.shared.data.MapObject.BY_NAME_COMPARATOR
								.compare(a.copy(), b.copy())));
			}
			assertEquals(a.name + " hashCode equal ids",
					a.java().hashCode() == a.java().hashCode(),
					a.copy().hashCode() == a.copy().hashCode());
		}
	}

	/** The gzipped values the indexer writes for long texts, decoded by two different unzippers. */
	@Test
	public void zippedContentIsTheSame() throws IOException {
		for (String text : new String[] {
				"a", "Ein kurzer Text", "Длинное описание маршрута, которое индексатор сжимает",
				repeat("The quick brown fox jumps over the lazy dog. ", 40),
				"line one\nline two\nline three", "trailing newline\n", ""}) {
			String zipped = zip(text);
			assertTrue("marked as zipped", MapObject.isContentZipped(zipped));
			assertEquals("zipped " + text.length(),
					net.osmand.shared.data.MapObject.isContentZipped(zipped),
					MapObject.isContentZipped(zipped));
			assertEquals("unzipped " + text.length(), MapObject.unzipContent(zipped),
					net.osmand.shared.data.MapObject.unzipContent(zipped));
			assertEquals("plain " + text.length(), MapObject.unzipContent(text),
					net.osmand.shared.data.MapObject.unzipContent(text));
		}
		assertEquals("null", MapObject.unzipContent(null),
				net.osmand.shared.data.MapObject.unzipContent(null));
		// a value that claims to be zipped but is not comes back as it was, on both sides
		assertEquals("not really zipped", MapObject.unzipContent(" gz not really"),
				net.osmand.shared.data.MapObject.unzipContent(" gz not really"));
	}

	@Test
	public void nameLangTagsAreTheSame() {
		for (String tag : new String[] {"name", "name:en", "name:de", "name:zh-hans", "name:sr-latn",
				"name:etymology", "name:etymology:wikidata", "ref", "name:abcd", "name:a-b"}) {
			assertEquals("isNameLangTag " + tag, MapObject.isNameLangTag(tag),
					net.osmand.shared.data.MapObject.isNameLangTag(tag));
		}
	}

	/** One amenity built twice, so that the two sides are given byte for byte the same input. */
	private static class Sample {
		final String name;
		private final String subType;
		private final String category;
		private final long id;
		private final String amenityName;
		private final String enName;
		private final Map<String, String> names = new LinkedHashMap<>();
		private final Map<String, String> additional = new LinkedHashMap<>();
		private final Map<Integer, List<String[]>> tagGroups = new LinkedHashMap<>();
		private final int[] x;
		private final int[] y;

		Sample(String name, String category, String subType, long id, String amenityName, String enName,
				int[] x, int[] y) {
			this.name = name;
			this.category = category;
			this.subType = subType;
			this.id = id;
			this.amenityName = amenityName;
			this.enName = enName;
			this.x = x;
			this.y = y;
		}

		Sample name(String lang, String value) {
			names.put(lang, value);
			return this;
		}

		Sample tag(String key, String value) {
			additional.put(key, value);
			return this;
		}

		Sample group(int id, String... tagValues) {
			List<String[]> pairs = new ArrayList<>();
			for (int i = 0; i < tagValues.length; i += 2) {
				pairs.add(new String[] {tagValues[i], tagValues[i + 1]});
			}
			tagGroups.put(id, pairs);
			return this;
		}

		Amenity java() {
			Amenity a = new Amenity();
			a.setId(id);
			a.setType(MapPoiTypes.getDefault().getPoiCategoryByName(category, true));
			a.setSubType(subType);
			a.setName(amenityName);
			a.setEnName(enName);
			for (Map.Entry<String, String> e : names.entrySet()) {
				a.setName(e.getKey(), e.getValue());
			}
			a.setLocation(50.1, 14.4);
			a.setFileOffset(4242);
			a.setRegionName("Czech Republic");
			a.setOrder(70);
			for (Map.Entry<String, String> e : additional.entrySet()) {
				a.setAdditionalInfo(e.getKey(), e.getValue());
			}
			for (Map.Entry<Integer, List<String[]>> e : tagGroups.entrySet()) {
				List<TagValuePair> pairs = new ArrayList<>();
				for (String[] tv : e.getValue()) {
					pairs.add(new TagValuePair(tv[0], tv[1], 0));
				}
				a.addTagGroup(e.getKey(), pairs);
			}
			if (x != null) {
				a.getX().add(x);
				a.getY().add(y);
				a.setBbox31(new int[] {x[0], y[0], x[x.length - 1], y[y.length - 1]});
			}
			return a;
		}

		net.osmand.shared.data.Amenity copy() {
			net.osmand.shared.data.Amenity a = new net.osmand.shared.data.Amenity();
			a.setId(id);
			a.setType(net.osmand.shared.osm.MapPoiTypes.getDefault().getPoiCategoryByName(category, true));
			a.setSubType(subType);
			a.setName(amenityName);
			a.setEnName(enName);
			for (Map.Entry<String, String> e : names.entrySet()) {
				a.setName(e.getKey(), e.getValue());
			}
			a.setLocation(50.1, 14.4);
			a.setFileOffset(4242);
			a.setRegionName("Czech Republic");
			a.setOrder(70);
			for (Map.Entry<String, String> e : additional.entrySet()) {
				a.setAdditionalInfo(e.getKey(), e.getValue());
			}
			for (Map.Entry<Integer, List<String[]>> e : tagGroups.entrySet()) {
				List<net.osmand.shared.binary.TagValuePair> pairs = new ArrayList<>();
				for (String[] tv : e.getValue()) {
					pairs.add(new net.osmand.shared.binary.TagValuePair(tv[0], tv[1], 0));
				}
				a.addTagGroup(e.getKey(), pairs);
			}
			if (x != null) {
				a.getX().add(x);
				a.getY().add(y);
				a.setBbox31(new int[] {x[0], y[0], x[x.length - 1], y[y.length - 1]});
			}
			return a;
		}
	}

	private static List<Sample> samples() {
		List<Sample> samples = new ArrayList<>();

		samples.add(new Sample("bakery", "shop", "bakery", 12345678L << 1, "Bäckerei", "Bakery", null, null)
				.name("de", "Bäckerei am Eck")
				.name("ru", "Пекарня")
				.tag("opening_hours", "Mo-Fr 07:00-18:00")
				.tag("phone", "+420 123 456 789")
				.tag("website", "https://example.org")
				.tag("addr_street", "Hlavní")
				.tag("addr_housenumber", "12")
				.tag("wikidata", "Q42")
				.tag("description", "A bakery")
				.tag("description:de", "Eine Bäckerei")
				.tag("cuisine", "bread")
				.group(1, "place", "city", "name", "Praha", "name:de", "Prag")
				.group(2, "place", "suburb", "name", "Žižkov"));

		samples.add(new Sample("route track", "routes", "routes_hiking", 7700604L, "Cesta hrdinov SNP",
				"Path of SNP Heroes", new int[] {1114000000, 1114001000}, new int[] {700000000, 700001000})
				.tag("route_id", "O7700604")
				.tag("route_bbox_radius", "25000")
				.tag("route_activity_type_hiking", "yes")
				.tag("route_name", "Cesta hrdinov SNP")
				.tag("ref", "E8")
				.tag("color", "red")
				.tag("gpx_icon", "special_trekking")
				.tag("description", "A long distance trail")
				.tag("description:uk", "Довгий маршрут")
				.tag("content", "Wikivoyage content")
				.tag("content:de", "Inhalt")
				.tag("short_description", "Trail")
				.tag("short_description:de", "Weg")
				.tag("wiki_lang:de", "yes")
				.group(1, "place", "town", "name", "Dukla"));

		samples.add(new Sample("super route", "routes", "route_track", 42L, "Via Alpina", null, null, null)
				.tag("route_id", "OSM123456")
				.tag("route_members_ids", "O1,O2,O3")
				.tag("route_activity_type_walking", "yes"));

		samples.add(new Sample("wiki article", "osmwiki", "route_article", 99L, "Praha", "Prague", null, null)
				.tag("travel_topic", "Europe")
				.tag("travel_elo", "1800")
				.tag("wiki_photo", "Prague_castle.jpg")
				.tag("wiki_category", "Cities")
				.tag("content_json", "{}")
				.tag("lang_yes:cs", "yes"));

		samples.add(new Sample("deleted", "shop", "butcher", 777L, "Řeznictví", null, null, null)
				.tag("osmand_change", "delete")
				.tag("access_private", "private"));

		samples.add(new Sample("no tags", "shop", "bakery", 88L, "", null, null, null));

		return samples;
	}

	private static String zip(String text) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(out);
		gz.write(text.getBytes(StandardCharsets.UTF_8));
		gz.close();
		byte[] bytes = out.toByteArray();
		StringBuilder sb = new StringBuilder(" gz ");
		for (byte b : bytes) {
			sb.append((char) (b + 128 + 32));
		}
		return sb.toString();
	}

	private static String repeat(String s, int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++) {
			sb.append(s);
		}
		return sb.toString();
	}
}
