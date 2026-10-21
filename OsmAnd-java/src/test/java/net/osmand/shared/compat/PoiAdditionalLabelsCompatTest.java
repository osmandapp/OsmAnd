package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiAdditionalLabels;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link net.osmand.shared.osm.PoiAdditionalLabels} against {@link PoiAdditionalLabels}, on the poi
 * types of both sides with the english phrases {@code SearchApisCompatTest} gives them: the search
 * names of every category, filter, type and additional value, the labels of every group of
 * additional values of a type, and what every phrase splits into.
 */
public class PoiAdditionalLabelsCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final net.osmand.shared.osm.PoiAdditionalLabels COPY = net.osmand.shared.osm.PoiAdditionalLabels.INSTANCE;

	@BeforeClass
	public static void setUp() throws Exception {
		SearchCompat.assumeEnabled();
		SearchApisCompatTest.setUpTypes();
	}

	@AfterClass
	public static void reset() {
		if (!SearchCompat.enabled()) {
			return;
		}
		// the names of the poi types were this test's
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
	}

	@Test
	public void typesAreLabelledTheSame() {
		List<PoiCategory> java = MapPoiTypes.getDefault().getCategories();
		List<net.osmand.shared.osm.PoiCategory> copy = net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getCategories();
		assertEquals("categories", java.size(), copy.size());
		int[] counts = new int[2];
		for (int i = 0; i < java.size(); i++) {
			PoiCategory jc = java.get(i);
			net.osmand.shared.osm.PoiCategory kc = copy.get(i);
			assertType(jc, kc, counts);
			assertAdditionals(jc, kc, counts);
			assertEquals(jc.getKeyName() + " filters", jc.getPoiFilters().size(), kc.getPoiFilters().size());
			for (int f = 0; f < jc.getPoiFilters().size(); f++) {
				assertType(jc.getPoiFilters().get(f), kc.getPoiFilters().get(f), counts);
			}
			assertEquals(jc.getKeyName() + " types", jc.getPoiTypes().size(), kc.getPoiTypes().size());
			for (int t = 0; t < jc.getPoiTypes().size(); t++) {
				PoiType jt = jc.getPoiTypes().get(t);
				net.osmand.shared.osm.PoiType kt = kc.getPoiTypes().get(t);
				assertType(jt, kt, counts);
				assertAdditionals(jt, kt, counts);
			}
		}
		assertTrue("types compared: " + counts[0], counts[0] > 10000);
		assertTrue("groups compared: " + counts[1], counts[1] > 1000);
		System.out.println("PoiAdditionalLabelsCompatTest: " + counts[0] + " types and " + counts[1]
				+ " groups of additional values labelled the same");
	}

	@Test
	public void phrasesSplitTheSame() {
		int compared = 0;
		for (String phrase : SearchApisCompatTest.poiPhrases.values()) {
			for (String text : phrase.split(";")) {
				String[] j = PoiAdditionalLabels.splitNameValue(text);
				String[] k = COPY.splitNameValue(text);
				assertEquals(text, j == null ? null : Arrays.asList(j), k == null ? null : Arrays.asList(k));
				assertEquals(text, PoiAdditionalLabels.getSwitchLabel(text, null), COPY.getSwitchLabel(text, null));
				assertEquals(text, PoiAdditionalLabels.getSearchName(text, "yes", phrase, null),
						COPY.getSearchName(text, "yes", phrase, null));
				compared++;
			}
		}
		assertTrue("phrases compared: " + compared, compared > 5000);
	}

	private static void assertType(AbstractPoiType j, net.osmand.shared.osm.AbstractPoiType k, int[] counts) {
		String m = j.getKeyName();
		assertEquals(m, j.getKeyName(), k.getKeyName());
		assertEquals(m + " search name", PoiAdditionalLabels.getSearchName(j), COPY.getSearchName(k));
		assertEquals(m + " en search name", PoiAdditionalLabels.getEnSearchName(j), COPY.getEnSearchName(k));
		if (j instanceof PoiType pt) {
			assertEquals(m + " accepted", PoiAdditionalLabels.isAcceptedValue(pt),
					COPY.isAcceptedValue((net.osmand.shared.osm.PoiType) k));
		}
		counts[0]++;
	}

	/** The additional values of a type, one by one and by the group they are listed in. */
	private static void assertAdditionals(AbstractPoiType j, net.osmand.shared.osm.AbstractPoiType k, int[] counts) {
		List<PoiType> ja = j.getPoiAdditionals();
		List<net.osmand.shared.osm.PoiType> ka = k.getPoiAdditionals();
		assertEquals(j.getKeyName() + " additionals", ja.size(), ka.size());
		Map<String, List<Integer>> groups = new LinkedHashMap<>();
		for (int i = 0; i < ja.size(); i++) {
			assertType(ja.get(i), ka.get(i), counts);
			groups.computeIfAbsent(String.valueOf(ja.get(i).getPoiAdditionalCategory()), g -> new ArrayList<>()).add(i);
		}
		for (Map.Entry<String, List<Integer>> group : groups.entrySet()) {
			List<String> translations = new ArrayList<>();
			List<String> values = new ArrayList<>();
			for (int i : group.getValue()) {
				translations.add(ja.get(i).getTranslation());
				values.add(ja.get(i).getOsmValue());
			}
			String m = j.getKeyName() + " group " + group.getKey();
			assertEquals(m, PoiAdditionalLabels.getGroupLabels(translations, values), COPY.getGroupLabels(translations, values));
			assertEquals(m, PoiAdditionalLabels.isSingleYes(values), COPY.isSingleYes(values));
			String groupName = ja.get(group.getValue().get(0)).getPoiAdditionalCategoryTranslation();
			assertEquals(m, PoiAdditionalLabels.getSwitchLabel(translations.get(0), groupName),
					COPY.getSwitchLabel(translations.get(0), groupName));
			counts[1]++;
		}
	}
}
