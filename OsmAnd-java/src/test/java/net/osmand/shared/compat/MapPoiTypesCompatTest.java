package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link net.osmand.shared.osm.MapPoiTypes} is a copy of {@link MapPoiTypes}; this reads the same
 * poi_types.xml with both and compares what they built out of it - every category, every filter,
 * every type and every additional attribute, field by field - and then asks both the same lookup
 * questions.
 *
 * Neither side has a {@code PoiTranslator}, so a type's name falls back to its key name spelled
 * out, which is itself worth comparing: it is what an untranslated type is shown as.
 */
public class MapPoiTypesCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";

	private static MapPoiTypes java;
	private static net.osmand.shared.osm.MapPoiTypes copy;
	private static int typesCompared;

	@BeforeClass
	public static void readBoth() {
		java = new MapPoiTypes(POI_TYPES);
		java.init();
		copy = new net.osmand.shared.osm.MapPoiTypes(POI_TYPES);
		copy.init();
	}

	@Test
	public void wholeTreeIsTheSame() {
		assertEquals("init", java.isInit(), copy.isInit());
		assertEquals("categories", java.getCategories().size(), copy.getCategories().size());
		for (int i = 0; i < java.getCategories().size(); i++) {
			assertCategory("category " + i, java.getCategories().get(i), copy.getCategories().get(i));
		}
		assertTrue("types compared: " + typesCompared, typesCompared > 10000);
		System.out.println("MapPoiTypesCompatTest: " + typesCompared + " poi types compared");
	}

	@Test
	public void theRegistryItselfIsTheSame() {
		assertEquals("other category", java.getOtherPoiCategory().getKeyName(),
				copy.getOtherPoiCategory().getKeyName());
		assertEquals("other map category", java.getOtherMapCategory().getKeyName(),
				copy.getOtherMapCategory().getKeyName());
		assertEquals("user defined", java.getUserDefinedCategory().getKeyName(),
				copy.getUserDefinedCategory().getKeyName());
		assertEquals("osmwiki", java.getOsmwiki().getKeyName(), copy.getOsmwiki().getKeyName());
		assertEquals("routes", java.getRoutes().getKeyName(), copy.getRoutes().getKeyName());
		assertEquals("wiki locales", java.getAllAvailableWikiLocales(), copy.getAllAvailableWikiLocales());
		assertEquals("public transport types", java.getPublicTransportTypes(), copy.getPublicTransportTypes());
		assertEquals("all languages suffix", java.getAllLanguagesTranslationSuffix(),
				copy.getAllLanguagesTranslationSuffix());
		assertEquals("categories without map", java.getCategories(false).size(),
				copy.getCategories(false).size());

		assertEquals("text additionals", keysOf(java.getTextPoiAdditionals()),
				copyKeysOf(copy.getTextPoiAdditionals()));

		List<String> javaTop = new ArrayList<>();
		for (AbstractPoiType t : java.getTopVisibleFilters()) {
			javaTop.add(t.getKeyName());
		}
		List<String> copyTop = new ArrayList<>();
		for (net.osmand.shared.osm.AbstractPoiType t : copy.getTopVisibleFilters()) {
			copyTop.add(t.getKeyName());
		}
		assertEquals("top visible filters", javaTop, copyTop);

		assertEquals("top index additionals", keysOfMap(java.topIndexPoiAdditional),
				copyKeysOfMap(copy.getTopIndexPoiAdditional()));
	}

	/** The lookups the reader actually calls, asked over every key the file defines. */
	@Test
	public void lookupsAnswerTheSame() {
		for (PoiCategory pc : java.getCategories()) {
			String cn = pc.getKeyName();
			assertEquals("category by name " + cn, pc.getKeyName(),
					copy.getPoiCategoryByName(cn).getKeyName());
			assertEquals("registered " + cn, java.isRegisteredType(pc),
					copy.isRegisteredType(copy.getPoiCategoryByName(cn)));
			for (PoiType pt : pc.getPoiTypes()) {
				String key = pt.getKeyName();
				assertSameType("by key " + key, java.getPoiTypeByKey(key), copy.getPoiTypeByKey(key));
				assertSameAbstract("any by key " + key, java.getAnyPoiTypeByKey(key),
						copy.getAnyPoiTypeByKey(key));
				assertSameAbstract("any by key no skip " + key, java.getAnyPoiTypeByKey(key, false),
						copy.getAnyPoiTypeByKey(key, false));
				assertSameAbstract("additional by key " + key,
						java.getAnyPoiAdditionalTypeByKey(key), copy.getAnyPoiAdditionalTypeByKey(key));
				assertSameType("in category " + key, java.getPoiTypeByKeyInCategory(pc, key),
						copy.getPoiTypeByKeyInCategory(copy.getPoiCategoryByName(cn), key));
				assertEquals("deprecated " + key, java.replaceDeprecatedSubtype(pc, key),
						copy.replaceDeprecatedSubtype(copy.getPoiCategoryByName(cn), key));
				assertSameType("by tag value " + key,
						java.getPoiTypeByTagValue(pt.getRawOsmTag(), pt.getOsmValue()),
						copy.getPoiTypeByTagValue(pt.getRawOsmTag(), pt.getOsmValue()));
				assertEquals("text additional info " + key,
						java.isTextAdditionalInfo(key, pt.getOsmValue()),
						copy.isTextAdditionalInfo(key, pt.getOsmValue()));
				for (PoiType add : pt.getPoiAdditionals()) {
					String ak = add.getKeyName();
					assertSameAbstract("additional by key " + ak,
							java.getAnyPoiAdditionalTypeByKey(ak), copy.getAnyPoiAdditionalTypeByKey(ak));
					assertSameType("text additional " + ak, java.getTextPoiAdditionalByKey(ak),
							copy.getTextPoiAdditionalByKey(ak));
				}
			}
			assertEquals("default tag " + cn, pc.getDefaultTag(),
					copy.getPoiCategoryByName(cn).getDefaultTag());
			assertSameType("default by tag " + cn, java.getDefaultPoiTypeByTag(pc.getDefaultTag()),
					copy.getDefaultPoiTypeByTag(pc.getDefaultTag()));
		}
		// the two aliases the lookup applies unless it is asked to create
		for (String alias : new String[] {"leisure", "historic", "shop", "no_such_category"}) {
			assertEquals("alias " + alias, java.getPoiCategoryByName(alias).getKeyName(),
					copy.getPoiCategoryByName(alias).getKeyName());
		}
		assertNull("missing additional", copy.getAnyPoiAdditionalTypeByKey("no_such_additional"));
		assertNull("missing additional in java", java.getAnyPoiAdditionalTypeByKey("no_such_additional"));
	}

	/** What a filter over a type accepts, which is how a poi search is narrowed down. */
	@Test
	public void acceptedTypesAreTheSame() {
		for (PoiCategory pc : java.getCategories()) {
			net.osmand.shared.osm.PoiCategory kpc = copy.getPoiCategoryByName(pc.getKeyName());
			assertEquals("category " + pc.getKeyName(), putTypes(pc), copyPutTypes(kpc));
			for (int i = 0; i < pc.getPoiFilters().size(); i++) {
				PoiFilter pf = pc.getPoiFilters().get(i);
				net.osmand.shared.osm.PoiFilter kpf = kpc.getPoiFilters().get(i);
				assertEquals("filter " + pf.getKeyName(), putTypes(pf), copyPutTypes(kpf));
			}
			for (int i = 0; i < pc.getPoiTypes().size(); i++) {
				PoiType pt = pc.getPoiTypes().get(i);
				net.osmand.shared.osm.PoiType kpt = kpc.getPoiTypes().get(i);
				assertEquals("type " + pt.getKeyName(), putTypes(pt), copyPutTypes(kpt));
			}
		}
	}

	/** Types the app forbids are dropped on both sides, and only while they are forbidden. */
	@Test
	public void forbiddenTypesAreTheSame() {
		Set<String> forbidden = new HashSet<>(Arrays.asList("bakery", "no_such_type"));
		MapPoiTypes javaForbidden = new MapPoiTypes(POI_TYPES);
		javaForbidden.setForbiddenTypes(forbidden);
		javaForbidden.init();
		net.osmand.shared.osm.MapPoiTypes copyForbidden = new net.osmand.shared.osm.MapPoiTypes(POI_TYPES);
		copyForbidden.setForbiddenTypes(forbidden);
		copyForbidden.init();

		for (String type : new String[] {"bakery", "no_such_type", "butcher"}) {
			assertEquals("forbidden " + type, javaForbidden.isTypeForbidden(type),
					copyForbidden.isTypeForbidden(type));
			assertEquals("dropped " + type, javaForbidden.getPoiTypeByKey(type) == null,
					copyForbidden.getPoiTypeByKey(type) == null);
		}
		assertNull("bakery is gone", javaForbidden.getPoiTypeByKey("bakery"));
		assertEquals("categories", javaForbidden.getCategories().size(),
				copyForbidden.getCategories().size());
	}

	private static void assertCategory(String m, PoiCategory j, net.osmand.shared.osm.PoiCategory k) {
		assertEquals(m + " keyName", j.getKeyName(), k.getKeyName());
		assertEquals(m + " ordinal", j.ordinal(), k.ordinal());
		assertEquals(m + " defaultTag", j.getDefaultTag(), k.getDefaultTag());
		assertEquals(m + " administrative", j.isAdministrative(), k.isAdministrative());
		assertEquals(m + " wiki", j.isWiki(), k.isWiki());
		assertEquals(m + " routes", j.isRoutes(), k.isRoutes());
		assertFilterFields(m, j, k);

		assertEquals(m + " filters", j.getPoiFilters().size(), k.getPoiFilters().size());
		for (int i = 0; i < j.getPoiFilters().size(); i++) {
			PoiFilter jf = j.getPoiFilters().get(i);
			net.osmand.shared.osm.PoiFilter kf = k.getPoiFilters().get(i);
			assertEquals(m + " filter " + i + " keyName", jf.getKeyName(), kf.getKeyName());
			assertEquals(m + " filter " + i + " category", jf.getPoiCategory().getKeyName(),
					kf.getPoiCategory().getKeyName());
			assertFilterFields(m + " filter " + jf.getKeyName(), jf, kf);
		}
	}

	/** The part of a filter that a category has too, since a category is one. */
	private static void assertFilterFields(String m, PoiFilter j, net.osmand.shared.osm.PoiFilter k) {
		assertAbstractFields(m, j, k);
		assertEquals(m + " types", j.getPoiTypes().size(), k.getPoiTypes().size());
		for (int i = 0; i < j.getPoiTypes().size(); i++) {
			PoiType jt = j.getPoiTypes().get(i);
			net.osmand.shared.osm.PoiType kt = k.getPoiTypes().get(i);
			assertPoiType(m + " type " + jt.getKeyName(), jt, kt);
			assertEquals(m + " by key " + jt.getKeyName(), jt.getKeyName(),
					k.getPoiTypeByKeyName(jt.getKeyName()).getKeyName());
			typesCompared++;
		}
	}

	private static void assertPoiType(String m, PoiType j, net.osmand.shared.osm.PoiType k) {
		assertAbstractFields(m, j, k);
		assertEquals(m + " reference", j.isReference(), k.isReference());
		if (j.isReference()) {
			assertEquals(m + " reference key", j.getReferenceType().getKeyName(),
					k.getReferenceType().getKeyName());
		}
		assertEquals(m + " additional", j.isAdditional(), k.isAdditional());
		if (j.isAdditional()) {
			assertEquals(m + " parent", j.getParentType().getKeyName(), k.getParentType().getKeyName());
		}
		assertEquals(m + " category", nameOf(j.getCategory()), copyNameOf(k.getCategory()));
		assertEquals(m + " filter", nameOf(j.getFilter()), copyNameOf(k.getFilter()));
		assertEquals(m + " osmTag", j.getOsmTag(), k.getOsmTag());
		assertEquals(m + " rawOsmTag", j.getRawOsmTag(), k.getRawOsmTag());
		assertEquals(m + " osmTag2", j.getOsmTag2(), k.getOsmTag2());
		assertEquals(m + " osmValue", j.getOsmValue(), k.getOsmValue());
		assertEquals(m + " osmValue2", j.getOsmValue2(), k.getOsmValue2());
		assertEquals(m + " editOsmTag", j.getEditOsmTag(), k.getEditOsmTag());
		assertEquals(m + " editOsmValue", j.getEditOsmValue(), k.getEditOsmValue());
		assertEquals(m + " editOsmTag2", j.getEditOsmTag2(), k.getEditOsmTag2());
		assertEquals(m + " editOsmValue2", j.getEditOsmValue2(), k.getEditOsmValue2());
		assertEquals(m + " osmTagsValues", j.getOsmTagsValues(), k.getOsmTagsValues());
		assertEquals(m + " filterOnly", j.isFilterOnly(), k.isFilterOnly());
		assertEquals(m + " text", j.isText(), k.isText());
		assertEquals(m + " nameTag", j.getNameTag(), k.getNameTag());
		assertEquals(m + " nameOnly", j.isNameOnly(), k.isNameOnly());
		assertEquals(m + " relation", j.isRelation(), k.isRelation());
		assertEquals(m + " order", j.getOrder(), k.getOrder());
		assertEquals(m + " topIndex", j.isTopIndex(), k.isTopIndex());
		assertEquals(m + " hidden", j.isHidden(), k.isHidden());
		assertEquals(m + " maxPerMap", j.getMaxPerMap(), k.getMaxPerMap());
		assertEquals(m + " minCount", j.getMinCount(), k.getMinCount());
		assertEquals(m + " defaultForCategory", j.isDefaultForCategory(), k.isDefaultForCategory());
		assertEquals(m + " toString", j.toString(), k.toString());
	}

	/** Everything the four kinds of poi type share. */
	private static void assertAbstractFields(String m, AbstractPoiType j,
			net.osmand.shared.osm.AbstractPoiType k) {
		assertEquals(m + " keyName", j.getKeyName(), k.getKeyName());
		assertEquals(m + " topVisible", j.isTopVisible(), k.isTopVisible());
		assertEquals(m + " notEditableOsm", j.isNotEditableOsm(), k.isNotEditableOsm());
		assertEquals(m + " nonIndx", j.isNonIndx(), k.isNonIndx());
		assertEquals(m + " lang", j.getLang(), k.getLang());
		assertEquals(m + " baseLangType", nameOf(j.getBaseLangType()), copyNameOf(k.getBaseLangType()));
		assertEquals(m + " iconKeyName", j.getIconKeyName(), k.getIconKeyName());
		assertEquals(m + " originalIconName", j.getOriginalIconName(), k.getOriginalIconName());
		assertEquals(m + " formattedKeyName", j.getFormattedKeyName(), k.getFormattedKeyName());
		assertEquals(m + " translation", j.getTranslation(), k.getTranslation());
		assertEquals(m + " enTranslation", j.getEnTranslation(), k.getEnTranslation());
		assertEquals(m + " synonyms", j.getSynonyms(), k.getSynonyms());
		assertEquals(m + " validTranslation", j.hasValidTranslation(), k.hasValidTranslation());
		assertEquals(m + " parentTypeName", j.getParentTypeName(), k.getParentTypeName());
		assertEquals(m + " additionalCategory", j.getPoiAdditionalCategory(), k.getPoiAdditionalCategory());
		assertEquals(m + " additionalCategoryTranslation", j.getPoiAdditionalCategoryTranslation(),
				k.getPoiAdditionalCategoryTranslation());
		assertEquals(m + " excludedCategories", j.getExcludedPoiAdditionalCategories(),
				k.getExcludedPoiAdditionalCategories());
		assertEquals(m + " additionals", keysOf(j.getPoiAdditionals()),
				copyKeysOf(k.getPoiAdditionals()));
		assertEquals(m + " categorized additionals", keysOf(j.getPoiAdditionalsCategorized()),
				copyKeysOf(k.getPoiAdditionalsCategorized()));
		for (int i = 0; i < j.getPoiAdditionals().size(); i++) {
			PoiType add = j.getPoiAdditionals().get(i);
			net.osmand.shared.osm.PoiType kadd = k.getPoiAdditionalByKeyName(add.getKeyName());
			assertNotNull(m + " additional " + add.getKeyName(), kadd);
			// the additionals are where the per language expansion lives, so they are worth
			// walking into rather than only listing
			assertPoiType(m + " additional " + add.getKeyName(), add, k.getPoiAdditionals().get(i));
			typesCompared++;
		}
	}

	private static void assertSameType(String m, PoiType j, net.osmand.shared.osm.PoiType k) {
		assertEquals(m + " present", j == null, k == null);
		if (j != null) {
			assertEquals(m, j.getKeyName(), k.getKeyName());
		}
	}

	private static void assertSameAbstract(String m, AbstractPoiType j,
			net.osmand.shared.osm.AbstractPoiType k) {
		assertEquals(m + " present", j == null, k == null);
		if (j != null) {
			assertEquals(m, j.getKeyName(), k.getKeyName());
		}
	}

	private static String nameOf(AbstractPoiType t) {
		return t == null ? null : t.getKeyName();
	}

	private static String copyNameOf(net.osmand.shared.osm.AbstractPoiType t) {
		return t == null ? null : t.getKeyName();
	}

	private static List<String> keysOf(List<PoiType> types) {
		List<String> keys = new ArrayList<>();
		for (PoiType t : types) {
			keys.add(t.getKeyName());
		}
		return keys;
	}

	private static List<String> copyKeysOf(List<net.osmand.shared.osm.PoiType> types) {
		List<String> keys = new ArrayList<>();
		for (net.osmand.shared.osm.PoiType t : types) {
			keys.add(t.getKeyName());
		}
		return keys;
	}

	private static Map<String, String> keysOfMap(Map<String, PoiType> types) {
		Map<String, String> keys = new LinkedHashMap<>();
		for (Map.Entry<String, PoiType> e : types.entrySet()) {
			keys.put(e.getKey(), e.getValue().getKeyName());
		}
		return keys;
	}

	private static Map<String, String> copyKeysOfMap(Map<String, net.osmand.shared.osm.PoiType> types) {
		Map<String, String> keys = new LinkedHashMap<>();
		for (Map.Entry<String, net.osmand.shared.osm.PoiType> e : types.entrySet()) {
			keys.put(e.getKey(), e.getValue().getKeyName());
		}
		return keys;
	}

	private static Map<String, Set<String>> putTypes(AbstractPoiType t) {
		Map<PoiCategory, LinkedHashSet<String>> accepted = t.putTypes(new LinkedHashMap<>());
		Map<String, Set<String>> plain = new LinkedHashMap<>();
		for (Map.Entry<PoiCategory, LinkedHashSet<String>> e : accepted.entrySet()) {
			plain.put(e.getKey().getKeyName(), e.getValue());
		}
		return plain;
	}

	private static Map<String, Set<String>> copyPutTypes(net.osmand.shared.osm.AbstractPoiType t) {
		Map<net.osmand.shared.osm.PoiCategory, LinkedHashSet<String>> accepted =
				t.putTypes(new LinkedHashMap<>());
		Map<String, Set<String>> plain = new LinkedHashMap<>();
		for (Map.Entry<net.osmand.shared.osm.PoiCategory, LinkedHashSet<String>> e : accepted.entrySet()) {
			plain.put(e.getKey().getKeyName(), e.getValue());
		}
		return plain;
	}
}
