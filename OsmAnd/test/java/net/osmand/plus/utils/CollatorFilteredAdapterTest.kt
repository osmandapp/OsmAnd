package net.osmand.plus.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.OsmAndCollator
import net.osmand.plus.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CollatorFilteredAdapterTest {

	@Test
	fun sortOrdersItemsAndFilterResults() {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		// unordered, as the Edit POI dialog gets the names from a HashMap
		val names = listOf("Magasin de vêtements", "Magasin de vidéos", "Boulangerie",
			"Magasin de vente en gros", "Magasin de vélos")
		val adapter = CollatorFilteredAdapter(instrumentation.targetContext, R.layout.list_textview, names)

		adapter.sort(OsmAndCollator.primaryCollator())
		assertEquals(listOf("Boulangerie", "Magasin de vélos", "Magasin de vente en gros",
			"Magasin de vêtements", "Magasin de vidéos"), getItems(adapter))

		val filtered = CountDownLatch(1)
		instrumentation.runOnMainSync { adapter.filter.filter("magasin de v") { filtered.countDown() } }
		assertTrue(filtered.await(5, TimeUnit.SECONDS))
		assertEquals(listOf("Magasin de vélos", "Magasin de vente en gros",
			"Magasin de vêtements", "Magasin de vidéos"), getItems(adapter))
	}

	private fun getItems(adapter: CollatorFilteredAdapter): List<String?> {
		return (0 until adapter.count).map { adapter.getItem(it) }
	}
}
