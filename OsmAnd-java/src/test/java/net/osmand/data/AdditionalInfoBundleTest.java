package net.osmand.data;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdditionalInfoBundleTest {

	@Test
	public void visibleTags_reflectFilteringAndCollapsingRules() {
		Map<String, String> tags = Map.ofEntries(
				entry("type", "sustenance"),
				entry("subtype", "cafe"),
				entry("addr_street", "Independence Ave"),
				entry("name", "Test Cafe"),
				entry("ref", "42"),
				entry("note", "Changed in 2020"),
				entry("cuisine", "italian"));
		AdditionalInfoBundle bundle = bundle(tags);

		List<AmenityTagEntry> visibleTags = bundle.getVisibleTags(false, List.of());
		assertNull("name is rendered separately, never as a tag row", findByKey(visibleTags, "name"));
		assertNull("hidden poi_additional must not be shown", findByKey(visibleTags, "addr_street"));
		assertNull("note is OSM-editing-only", findByKey(visibleTags, "note"));
		assertEquals("42", findByKey(visibleTags, "ref").value);
		AmenityTagEntry cuisine = findByKey(visibleTags, "cuisine");
		assertEquals("plain cuisine tag is shown when no collapsable group is present",
				AmenityTagEntry.CollapsableEntryType.NONE, cuisine.collapsableEntryType);
		assertEquals("italian", cuisine.value);

		List<AmenityTagEntry> visibleTagsWithEditing = bundle.getVisibleTags(true, List.of());
		assertEquals("Changed in 2020", findByKey(visibleTagsWithEditing, "note").value);
	}

	@Test
	public void collapsableCuisineGroup_preservesRecordOrder_andSuppressesPlainCuisineTag() {
		AdditionalInfoBundle bundle = bundle(Map.ofEntries(
				entry("type", "sustenance"),
				entry("subtype", "cafe"),
				entry("cuisine", "mexican"),
				entry("collapsable_cuisine", "cuisine_mexican;cuisine_italian")));

		List<AmenityTagEntry> visibleTags = bundle.getVisibleTags(false, List.of());

		List<AmenityTagEntry> cuisineEntries = visibleTags.stream()
				.filter(e -> "cuisine".equals(e.key))
				.toList();
		assertEquals("only the collapsable group must remain, the plain cuisine row is suppressed",
				1, cuisineEntries.size());

		AmenityTagEntry group = cuisineEntries.get(0);
		assertEquals(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP, group.collapsableEntryType);
		List<String> orderedKeys = group.collapsablePoiTypes.stream()
				.map(PoiType::getKeyName)
				.collect(Collectors.toList());
		assertEquals("group must preserve the order given in the tag value, not XML declaration order",
				List.of("cuisine_mexican", "cuisine_italian"), orderedKeys);
	}

	@Test
	public void visibleTags_canBeSortedByOrder_usingAmenityTagEntriesBuilder() {
		AdditionalInfoBundle bundle = bundle(Map.ofEntries(
				entry("type", "sustenance"),
				entry("subtype", "cafe"),
				entry("wiki_link", "http://example.com"), // order 95
				entry("ref", "42"),                       // order 50
				entry("from", "9:00"),                    // order 79
				entry("to", "18:00")));                   // order 78

		List<AmenityTagEntry> visibleTags = bundle.getVisibleTags(false, List.of());
		AmenityTagEntriesBuilder.sortInfoEntries(visibleTags);

		List<String> orderedKeys = visibleTags.stream().map(e -> e.key).collect(Collectors.toList());
		assertEquals(List.of("ref", "to", "from", "wiki_link"), orderedKeys);
	}

	private static AdditionalInfoBundle bundle(Map<String, String> tags) {
		return new AdditionalInfoBundle(MapPoiTypes.getDefault(), tags);
	}

	private static AmenityTagEntry findByKey(List<AmenityTagEntry> entries, String key) {
		return entries.stream().filter(e -> key.equals(e.key)).findFirst().orElse(null);
	}
}
