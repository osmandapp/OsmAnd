package net.osmand.shared.data

import net.osmand.shared.data.AmenityTagEntry.CollapsableEntryType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

// The rows of a point card that Android, iOS and the web server build from the tags of a POI or a GPX point.
class AdditionalInfoBundleTest {

	private var poiTypes: MapPoiTypes? = null

	@BeforeTest
	fun loadPoiTypes() {
		val path = poiTypesFile()
		if (path == null) {
			// Kotlin/Native runs from a simulator: it needs OSMAND_TEST_RESOURCES to find the file
			if (testPlatformName().startsWith("jvm")) {
				fail("poi_types.xml not found in ${directories()}")
			}
			println("AdditionalInfoBundleTest: poi_types.xml not found in ${directories()}, POI checks skipped")
			return
		}
		poiTypes = MapPoiTypes(path).also {
			it.init()
			MapPoiTypes.setDefault(it)
		}
	}

	@Test
	fun visibleTagsReflectFilteringRules() {
		val bundle = bundle(
			"type" to "sustenance", "subtype" to "cafe", "addr_street" to "Independence Ave", "name" to "Test Cafe",
			"ref" to "42", "note" to "Changed in 2020", "cuisine" to "italian"
		) ?: return

		val visibleTags = bundle.getVisibleTags(false, emptyList())
		assertNull(find(visibleTags, "name"), "name is rendered separately, never as a tag row")
		assertNull(find(visibleTags, "addr_street"), "hidden poi_additional must not be shown")
		assertNull(find(visibleTags, "note"), "note is OSM-editing-only")
		assertEquals("42", find(visibleTags, "ref")?.value)
		val cuisine = find(visibleTags, "cuisine")!!
		assertEquals(CollapsableEntryType.NONE, cuisine.collapsableEntryType)
		assertEquals("italian", cuisine.value)

		assertEquals("Changed in 2020", find(bundle.getVisibleTags(true, emptyList()), "note")?.value)
	}

	@Test
	fun skippedKeysAreNotShown() {
		val bundle = bundle(
			"type" to "sustenance", "subtype" to "fast_food", "route_id" to "1", "wikidata" to "Q1", "image" to "x",
			"wikipedia" to "w", "content" to "c", "short_description" to "s", "osmand_socket_cee_blue_yes" to "yes",
			"color" to "#fff", "icon" to "x", "address" to "a", "level" to "0", "ref" to "7"
		) ?: return
		bundle.setCustomHiddenExtensions(listOf("level"))

		assertEquals(listOf("ref"), bundle.getVisibleTags(false, emptyList()).map { it.key })
	}

	@Test
	fun collapsableCuisineGroupKeepsRecordOrderAndSuppressesPlainCuisine() {
		val bundle = bundle(
			"type" to "sustenance", "subtype" to "cafe", "cuisine" to "mexican",
			"collapsable_cuisine" to "cuisine_mexican;cuisine_italian"
		) ?: return

		val cuisineEntries = bundle.getVisibleTags(false, emptyList()).filter { it.key == "cuisine" }
		assertEquals(1, cuisineEntries.size, "only the collapsable group must remain")
		val group = cuisineEntries[0]
		assertEquals(CollapsableEntryType.POI_TYPE_GROUP, group.collapsableEntryType)
		assertEquals(listOf("cuisine_mexican", "cuisine_italian"), group.collapsablePoiTypes?.map { it.getKeyName() })
	}

	@Test
	fun storedCollapsableGroupBecomesPoiAdditionalGroup() {
		val bundle = bundle(
			"type" to "sustenance", "subtype" to "fast_food",
			"collapsable_payment_type" to "payment_cash_yes;payment_visa_yes"
		) ?: return

		val group = single(bundle.getVisibleTags(false, emptyList()))
		assertEquals("payment_type", group.key)
		assertEquals(CollapsableEntryType.POI_TYPE_GROUP, group.collapsableEntryType)
		assertEquals(listOf("payment_cash_yes", "payment_visa_yes"), group.collapsablePoiTypes?.map { it.getKeyName() })
		assertTrue(group.poiAdditional)
		assertEquals("sustenance", group.collapsableCategory?.getKeyName())
	}

	@Test
	fun typesOfAnotherCategoryBecomeCategoryGroup() {
		val bundle = bundle(
			"type" to "osmwiki", "subtype" to "wiki_place", "attraction" to "attraction",
			"orthodox_church" to "orthodox_church"
		) ?: return

		val group = single(bundle.getVisibleTags(false, emptyList()))
		assertEquals("tourism", group.key)
		assertEquals(CollapsableEntryType.POI_TYPE_GROUP, group.collapsableEntryType)
		assertEquals(listOf("attraction", "orthodox_church"), group.collapsablePoiTypes?.map { it.getKeyName() })
		assertEquals(false, group.poiAdditional)
		assertEquals(40, group.order)
	}

	@Test
	fun localizedTagTakesPreferredLanguageAsHeader() {
		val tags = arrayOf(
			"type" to "sustenance", "subtype" to "fast_food", "brand" to "McD", "brand:en" to "McD en",
			"brand:uk" to "McD uk"
		)
		val en = single(bundle(*tags)?.getVisibleTags(false, listOf("en")) ?: return)
		assertEquals("brand:en", en.key)
		assertEquals("McD en", en.value)
		assertEquals(CollapsableEntryType.PLAIN, en.collapsableEntryType)
		assertEquals(listOf("brand:uk=McD uk", "brand=McD"), en.collapsableEntries?.map { "${it.key}=${it.value}" })

		val noPreferred = single(bundle(*tags)!!.getVisibleTags(false, listOf("de")))
		assertEquals("brand:en", noPreferred.key, "without the preferred language the first variant is the header")
	}

	@Test
	fun unknownKeyGetsGenericRowOnlyWhenAllowed() {
		val tags = arrayOf("type" to "sustenance", "subtype" to "fast_food", "my_note" to "note", "gpxx:city" to "Town")
		assertEquals(emptyList(), bundle(*tags)?.getVisibleTags(false, emptyList())?.map { it.key } ?: return)

		val rows = bundle(*tags)!!.getVisibleTags(false, emptyList(), setOf("my_note", "gpxx:city"))
		assertEquals(listOf("my_note", "gpxx:city"), rows.map { it.key })
		assertTrue(rows.all { it.order == 90 })
	}

	@Test
	fun colonKeyResolvesThroughUnderscoreKey() {
		val row = single(bundle("type" to "sustenance", "subtype" to "fast_food", "internet_access:fee" to "no")
			?.getVisibleTags(false, emptyList()) ?: return)
		assertEquals("internet_access_fee_no", row.resolvedType?.additionalType?.getKeyName())
	}

	@Test
	fun prefixedKeysOfAStoredPointAreRead() {
		val bundle = bundle(
			"amenity_opening_hours" to "Mo 9-5", "osm_tag_ref" to "7", "amenity_name" to "X", "amenity_type" to "sustenance"
		) ?: return
		assertEquals("user_defined_other", bundle.getCategory()?.getKeyName())
		assertEquals(listOf("opening_hours", "ref"), bundle.getVisibleTags(false, emptyList()).map { it.key })
	}

	@Test
	fun descriptionRowIsMarked() {
		val rows = bundle("type" to "sustenance", "subtype" to "fast_food", "description" to "Text", "ref" to "7")
			?.getVisibleTags(false, emptyList()) ?: return
		assertTrue(find(rows, "description")!!.isDescription)
		assertEquals(false, find(rows, "ref")!!.isDescription)
	}

	@Test
	fun entriesSortByOrder() {
		val rows = bundle(
			"type" to "sustenance", "subtype" to "cafe", "wiki_link" to "http://example.com", "ref" to "42",
			"from" to "9:00", "to" to "18:00"
		)?.getVisibleTags(false, emptyList()) ?: return
		AmenityTagEntriesBuilder.sortInfoEntries(rows)
		assertEquals(listOf("ref", "to", "from", "wiki_link"), rows.map { it.key })
	}

	@Test
	fun preferredDescriptionMovesFirst() {
		val rows = mutableListOf(
			AmenityTagEntry.Builder("description").setValue("d").build(),
			AmenityTagEntry.Builder("description:de").setValue("de").build()
		)
		AmenityTagEntriesBuilder.sortDescriptionEntries(rows, "de")
		assertEquals(listOf("description:de", "description"), rows.map { it.key })
	}

	@Test
	fun genericRowKeysSkipOsmAndFields() {
		val stored = mapOf(
			"my_note" to "1", "displaymode" to "S", "test:country" to "US", "gpxx:city" to "T",
			"hidden" to "true", "visited_date" to "d", "creation_date" to "d", "pickup_date" to "d",
			"calendar_event" to "true", "offset" to "1", "pinned" to "true", "width" to "2", "ele" to "1",
			"speed" to "1", "trkpt_idx" to "1", "point_type" to "p", "color" to "c", "icon" to "i",
			"background" to "b", "address" to "a", "amenity_origin" to "o", "origin" to "o", "osm_url" to "u",
			"osmand:activity" to "a", "gpxtpx:hr" to "1", "amenity_type" to "t", "osm_tag_ref" to "1"
		)
		assertEquals(
			setOf("my_note", "displaymode", "test:country", "gpxx:city"),
			AdditionalInfoBundle.getGenericRowKeys(stored)
		)
	}

	private fun bundle(vararg tags: Pair<String, String>): AdditionalInfoBundle? =
		poiTypes?.let { AdditionalInfoBundle(it, linkedMapOf(*tags)) }

	private fun find(entries: List<AmenityTagEntry>, key: String): AmenityTagEntry? = entries.firstOrNull { it.key == key }

	private fun single(entries: List<AmenityTagEntry>): AmenityTagEntry {
		assertEquals(1, entries.size, entries.map { it.key }.toString())
		return entries[0]
	}

	private fun directories(): List<String> =
		listOfNotNull(testEnvironment("OSMAND_TEST_RESOURCES")) + listOf("../OsmAnd-java/src/test/resources", "OsmAnd-java/src/test/resources")

	private fun poiTypesFile(): String? = directories()
		.map { "$it/poi_types.xml".toPath() }
		.firstOrNull { FileSystem.SYSTEM.exists(it) }
		?.toString()
}
