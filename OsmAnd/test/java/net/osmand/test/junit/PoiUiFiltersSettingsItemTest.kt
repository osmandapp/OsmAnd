package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.osm.PoiCategory
import net.osmand.plus.OsmandApplication
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.settings.backend.backup.items.PoiUiFiltersSettingsItem
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PoiUiFiltersSettingsItemTest {

	private lateinit var app: OsmandApplication

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
		assertFalse("application initialization timed out", app.isApplicationInitializing)
	}

	@Test
	fun readsLegacyStringCategoryKeys() {
		val json = JSONObject("""
			{
				"items": [{
					"name": "Places to eat",
					"filterId": "user_places_to_eat",
					"filterByName": "terrace",
					"acceptedTypes": "{\"sustenance\":[\"restaurant\",\"cafe\"],\"tourism\":[]}"
				}]
			}
		""".trimIndent())

		val filter = read(json).single()

		assertEquals("Places to eat", filter.name)
		assertEquals("user_places_to_eat", filter.filterId)
		assertEquals("terrace", filter.filterByName)
		assertEquals(2, filter.acceptedTypes.size)
		assertEquals(listOf("restaurant", "cafe"),
			filter.acceptedTypes[category("sustenance")]!!.toList())
		assertEquals(emptySet<String>(), filter.acceptedTypes[category("tourism")])
	}

	@Test
	fun writesTheExistingSchemaAndRoundTripsAcceptedTypes() {
		val filter = PoiUIFilter("Places to eat", "user_places_to_eat", linkedMapOf(
			category("sustenance") to linkedSetOf("restaurant", "cafe"),
			category("tourism") to linkedSetOf<String>()
		), app)
		filter.setFilterByName("terrace")

		val json = write(filter)
		val item = json.getJSONArray("items").getJSONObject(0)

		// acceptedTypes is a JSON string containing an object, as in legacy exports.
		assertTrue(item.get("acceptedTypes") is String)
		assertEquals("{\"sustenance\":[\"restaurant\",\"cafe\"],\"tourism\":[]}",
			item.getString("acceptedTypes"))
		assertEquals(filter.name, item.getString("name"))
		assertEquals(filter.filterId, item.getString("filterId"))
		assertEquals(filter.filterByName, item.getString("filterByName"))

		val restored = read(json).single()
		assertEquals(filter.name, restored.name)
		assertEquals(filter.filterId, restored.filterId)
		assertEquals(filter.filterByName, restored.filterByName)
		assertEquals(filter.acceptedTypes, restored.acceptedTypes)
		assertEquals(listOf("restaurant", "cafe"),
			restored.acceptedTypes[category("sustenance")]!!.toList())
	}

	@Test
	fun expandsAWholeCategoryToOrderedSubtypeNames() {
		val category = category("sustenance")
		val filter = PoiUIFilter("All food", "user_all_food", linkedMapOf(category to null), app)
		val expected = category.poiTypes.map { it.keyName }.distinct()
		assertTrue("the category must contain types to exercise expansion", expected.isNotEmpty())

		val json = write(filter)
		val acceptedTypes = JSONObject(json.getJSONArray("items").getJSONObject(0).getString("acceptedTypes"))
		val types = acceptedTypes.getJSONArray(category.keyName)

		assertEquals(expected, (0 until types.length()).map { types.getString(it) })
		assertEquals(expected, read(json).single().acceptedTypes[category]!!.toList())
	}

	@Test
	fun writesStableKeysWithoutStringifyingCategoryObjects() {
		val category = object : PoiCategory(app.poiTypes, "sustenance", 0) {
			override fun toString(): String = throw AssertionError("Serialize the category's stable key")
		}
		val filter = PoiUIFilter("Cafe", "user_cafe", linkedMapOf(category to linkedSetOf("cafe")), app)

		val json = write(filter)

		assertEquals("{\"sustenance\":[\"cafe\"]}",
			json.getJSONArray("items").getJSONObject(0).getString("acceptedTypes"))
	}

	private fun category(key: String): PoiCategory = app.poiTypes.getPoiCategoryByName(key).also {
		assertEquals(key, it.keyName)
		assertEquals(it.keyName, it.toString())
	}

	private fun write(filter: PoiUIFilter): JSONObject {
		val item = PoiUiFiltersSettingsItem(app, listOf(filter))
		val output = ByteArrayOutputStream()
		item.writer!!.writeToStream(output, null)
		assertTrue(item.warnings.toString(), item.warnings.isEmpty())
		return JSONObject(output.toString("UTF-8"))
	}

	private fun read(json: JSONObject): List<PoiUIFilter> {
		val item = PoiUiFiltersSettingsItem(app, mutableListOf<PoiUIFilter>())
		ByteArrayInputStream(json.toString().toByteArray(Charsets.UTF_8)).use {
			item.reader!!.readFromStream(it, null, null)
		}
		assertTrue(item.warnings.toString(), item.warnings.isEmpty())
		return item.items
	}
}
