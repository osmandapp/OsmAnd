package net.osmand.plus.gallery.attached

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.shared.gpx.primitives.Link
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Media row of a favourite must show its current links when the card opens, also after the
 * links were changed outside the gallery - deleted in My Places > A/V notes or replaced by an
 * OsmAnd Cloud sync (#26798). The row used to show the list cached when the card was opened
 * before, until the app restarted.
 *
 * The point exists only in this test: the user's favourites are never loaded, changed or saved.
 */
@RunWith(AndroidJUnit4::class)
class AttachedMediaRowControllerTest {

	companion object {
		private const val HREF = "file:///test-26798/photo.jpg"
	}

	private lateinit var app: OsmandApplication
	private lateinit var point: FavouritePoint
	private lateinit var key: GalleryKey

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
		point = FavouritePoint(0.0, 0.0, "test-26798", "test-26798")
		key = GalleryKey.Favorite(point.key)
	}

	@After
	fun cleanup() {
		app.galleryHelper.repository.invalidate(key)
		app.galleryHelper.attachedMediaRegistry.unregister(key)
	}

	@Test
	fun rowShowsLinksChangedOutsideGallery() {
		val link = Link(HREF)
		point.addLink(link)
		assertTrue(openRowHasMedia())

		point.removeLink(link)
		assertFalse(openRowHasMedia())
	}

	private fun openRowHasMedia(): Boolean {
		var hasMedia = false
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			app.galleryHelper.attachedMediaRegistry.register(key, point)
			val controller = AttachedMediaRowController(app, key, point, null)
			controller.onRowBuilt(false)
			hasMedia = controller.hasMedia()
			controller.detach()
		}
		return hasMedia
	}
}
