package net.osmand.plus.gallery.library

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.track.GpxSelectionParams
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.media.LinkMediaFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Regression test for #26833: My Places > Media listed a link to a local file that no longer
 * exists, and tapping it failed with "Could not play media".
 *
 * The links sit on a point of a temporary track that is selected in memory only (the selection is
 * not saved): track points and favorites take the same path in [MediaLibraryScanner].
 */
@RunWith(AndroidJUnit4::class)
class MediaLibraryScannerTest {

	companion object {
		private const val TYPE = "audio/3gpp"
		private const val MISSING_FILE_NAME = "test-26833-missing.3gp"
		private const val REMOTE_HREF = "https://example.com/test-26833.3gp"
	}

	private lateinit var app: OsmandApplication
	private lateinit var dir: File
	private lateinit var gpx: GpxFile

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext
			.applicationContext as OsmandApplication
		dir = File(app.cacheDir, "test-26833")
		dir.mkdirs()
		gpx = GpxFile(null).apply { path = File(dir, "track.gpx").absolutePath }
	}

	@After
	fun cleanup() {
		app.selectedGpxHelper.selectGpxFile(gpx, GpxSelectionParams.newInstance().hideFromMap())
		dir.deleteRecursively()
	}

	@Test
	fun linkToMissingLocalFileIsNotListed() {
		val present = File(dir, "present.3gp").apply { writeBytes(ByteArray(16)) }
		val presentHref = Uri.fromFile(present).toString()
		val missingHref = LinkMediaFactory.createInternalMediaUri(MISSING_FILE_NAME)
		val point = WptPt(50.0, 4.0)
		for (href in listOf(presentHref, missingHref, REMOTE_HREF)) {
			point.addLink(Link(href, null, TYPE))
		}
		gpx.addPoint(point)
		app.selectedGpxHelper.selectGpxFile(gpx, GpxSelectionParams.newInstance().showOnMap())

		val listed = MediaLibraryScanner(app, null).scan()
			.flatMap { it.attachments }
			.filter { it.trackFile == gpx.path }
			.map { it.link.href }
		assertEquals(setOf(presentHref, REMOTE_HREF), listed.toSet())
	}
}
