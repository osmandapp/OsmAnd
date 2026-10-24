package net.osmand.plus.myplaces.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.shared.SharedUtil
import net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME
import net.osmand.shared.gpx.primitives.Link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Regression test for media attached to favourites from My Places > Media (OsmAnd-Issues#3283).
 *
 * `SaveFavoritesTask` rewrites a `favorites-*.gpx` group file only when the group in memory differs
 * from the one read from that file, and `FavouritePoint.equals()` ignores links. Attaching or
 * detaching media changes nothing but links, so only the internal file was updated and the group
 * files - whose modification time Cloud sync checks - kept the old links.
 *
 * Groups are written to and read from a temporary file only: the user's favourites and their
 * files are never loaded, changed or saved.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesLinksSaveTest {

	companion object {
		private const val GROUP_NAME = "test-3283-links"
		private const val HREF = "file:///test-3283/photo.jpg"
	}

	private lateinit var app: OsmandApplication
	private lateinit var fileHelper: FavouritesFileHelper
	private lateinit var file: File

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
		fileHelper = app.favoritesHelper.fileHelper
		file = File(app.cacheDir, "test-3283/$GROUP_NAME.gpx")
		file.parentFile?.mkdirs()
	}

	@After
	fun cleanup() {
		file.parentFile?.deleteRecursively()
	}

	@Test
	fun pointsWithDifferentLinksAreNotLinksEqual() {
		val first = point()
		val second = point()
		assertTrue(first.linksEqual(second))

		second.addLink(Link(HREF))
		assertFalse(first.linksEqual(second))

		first.addLink(Link(HREF, "note"))
		assertFalse(first.linksEqual(second))

		second.links!![0].text = "note"
		assertTrue(first.linksEqual(second))
		// equals() and hashCode() stay as they were: links are not part of them
		assertEquals(first, point())
	}

	@Test
	fun linksThatAreNotSavedDoNotMakePointsDifferent() {
		val first = point()
		val second = point()
		// Blank hrefs are not written to GPX, and empty text or type is read back as null.
		first.addLink(Link(" "))
		first.addLink(Link(HREF, "", ""))
		second.addLink(Link(HREF))
		assertTrue(first.linksEqual(second))
	}

	@Test
	fun groupReadBackWithItsLinksIsNotChanged() {
		val local = loaded(point())
		local.points[0].apply {
			addLink(Link(" "))
			addLink(Link(HREF, "", ""))
			addLink(Link("$HREF?2", "Kyiv view", "image/jpeg"))
		}
		// An unchanged group must not be rewritten, or every save would upload it to Cloud again.
		assertFalse(SaveFavoritesTask.isGroupChanged(local, readBack(local)))
	}

	@Test
	fun linksOnlyChangeChangesGroup() {
		val local = loaded(point())
		val point = local.points[0]
		val fileGroup = readBack(local)
		assertFalse(SaveFavoritesTask.isGroupChanged(local, fileGroup))

		// What AttachedMediaDataHelper.addMediaLinks() does.
		point.addLink(Link(HREF))
		assertTrue("attached link would not be saved", SaveFavoritesTask.isGroupChanged(local, fileGroup))

		// What AttachedMediaDataHelper.removeMediaLinks() does.
		val fileGroupWithLink = readBack(local)
		assertFalse(SaveFavoritesTask.isGroupChanged(local, fileGroupWithLink))
		point.removeLink(point.links!![0])
		assertNull(point.links?.firstOrNull())
		assertTrue("detached link would stay saved", SaveFavoritesTask.isGroupChanged(local, fileGroupWithLink))
	}

	private fun readBack(group: FavoriteGroup): FavoriteGroup {
		assertNull(fileHelper.saveFile(listOf(group), file))
		val groups = LinkedHashMap<String, FavoriteGroup>()
		fileHelper.collectFavoriteGroups(SharedUtil.loadGpxFile(file), groups)
		val result = groups[GROUP_NAME]
		assertNotNull("group is missing from the written file", result)
		return result!!
	}

	/** The group as the app holds it after loading it from its file. */
	private fun loaded(point: FavouritePoint): FavoriteGroup =
		readBack(FavoriteGroup(GROUP_NAME, arrayListOf(point), 0, true, false))

	/**
	 * The icon and background are set so that the point equals the one read back from its file,
	 * which gets the defaults for both.
	 */
	private fun point(): FavouritePoint {
		val point = FavouritePoint(50.0, 4.0, "Point", GROUP_NAME, 10.0, 1_000L)
		point.setIconIdFromName(DEFAULT_ICON_NAME)
		point.backgroundType = FavouritePoint.DEFAULT_BACKGROUND_TYPE
		return point
	}
}
