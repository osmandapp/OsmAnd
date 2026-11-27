package net.osmand.plus.mapcontextmenu.builders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.data.Amenity
import net.osmand.data.Amenity.DESCRIPTION
import net.osmand.data.Amenity.SHORT_DESCRIPTION
import net.osmand.data.Amenity.WIKIPEDIA
import net.osmand.data.BaseDetailsObject
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.shared.data.AdditionalInfoBundle
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A place with its own description keeps its Wikipedia row above the description (#26708):
 * the offline article excerpt, or the online article link without the Wikipedia map.
 */
@RunWith(AndroidJUnit4::class)
class PlaceDetailsDescriptionTest : AndroidTest() {

	companion object {
		private const val POI_TYPES_INIT_TIMEOUT_MS = 30_000L
		private const val ARTICLE = "Wasserkuppe"
		private const val WIKIPEDIA_URL = "https://de.wikipedia.org/wiki/$ARTICLE"
		private const val EXCERPT = "The highest mountain of the Rhön range."
		private const val OSM_DESCRIPTION = "Der höchste Berg der Rhön."
	}

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@Test
	fun mapCardShowsOnlineWikipediaAboveDescription() {
		val texts = buildPlaceDetails(mapOf(DESCRIPTION to OSM_DESCRIPTION, WIKIPEDIA to WIKIPEDIA_URL))

		assertInOrder(texts, ARTICLE, readOnWikipedia(), OSM_DESCRIPTION)
	}

	@Test
	fun mapCardShowsArticleExcerptAboveDescription() {
		val texts = buildPlaceDetails(mapOf(DESCRIPTION to OSM_DESCRIPTION,
				SHORT_DESCRIPTION to EXCERPT, WIKIPEDIA to WIKIPEDIA_URL))

		assertInOrder(texts, EXCERPT, app.getString(R.string.context_menu_read_full_article), OSM_DESCRIPTION)
	}

	@Test
	fun poiCardShowsArticleExcerptAboveDescription() {
		val texts = buildAmenityCard(mapOf(DESCRIPTION to OSM_DESCRIPTION,
				SHORT_DESCRIPTION to EXCERPT, WIKIPEDIA to WIKIPEDIA_URL))

		assertInOrder(texts, EXCERPT, app.getString(R.string.context_menu_read_full_article), OSM_DESCRIPTION)
	}

	private fun buildPlaceDetails(tags: Map<String, String>): List<String> {
		val detailsObject = BaseDetailsObject(createPeak(tags), "en")
		return buildDescription { PlaceDetailsMenuBuilder(it, detailsObject) }
	}

	private fun buildAmenityCard(tags: Map<String, String>): List<String> {
		val peak = createPeak(tags)
		return buildDescription { AmenityMenuBuilder(it, peak) }
	}

	private fun buildDescription(createBuilder: (MapActivity) -> AmenityMenuBuilder): List<String> {
		var texts = emptyList<String>()
		scenarioRule.scenario.onActivity { activity ->
			val builder = createBuilder(activity)
			val extensions = builder.amenity.getAmenityExtensions(app.poiTypes, false)
			builder.infoBundle = AdditionalInfoBundle(app.kPoiTypes, extensions)
			val container = LinearLayout(activity)
			container.orientation = LinearLayout.VERTICAL
			builder.buildDescription(container)
			texts = collectTexts(container)
		}
		return texts
	}

	private fun createPeak(tags: Map<String, String>): Amenity {
		waitForPoiTypes()
		val amenity = Amenity()
		amenity.type = app.poiTypes.getPoiCategoryByName("natural")
		amenity.subType = "peak"
		amenity.name = ARTICLE
		amenity.setLocation(50.49765, 9.93629)
		amenity.setAdditionalInfo(tags)
		return amenity
	}

	private fun readOnWikipedia(): String {
		return app.getString(R.string.read_on, app.getString(R.string.shared_string_wikipedia))
	}

	private fun collectTexts(view: View): List<String> = when (view) {
		is TextView -> listOf(view.text.toString())
		is ViewGroup -> (0 until view.childCount).flatMap { collectTexts(view.getChildAt(it)) }
		else -> emptyList()
	}

	private fun assertInOrder(texts: List<String>, vararg expected: String) {
		val positions = expected.map { text -> texts.indexOfFirst { it.contains(text) } }
		assertTrue("expected ${expected.toList()} in this order, the card has $texts",
				positions.none { it < 0 } && positions.zipWithNext().all { (a, b) -> a < b })
	}

	private fun waitForPoiTypes() {
		val deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS
		while (!app.poiTypes.isInit && System.currentTimeMillis() < deadline) {
			Thread.sleep(50)
		}
		assertTrue("POI types are not initialized", app.poiTypes.isInit)
	}
}
