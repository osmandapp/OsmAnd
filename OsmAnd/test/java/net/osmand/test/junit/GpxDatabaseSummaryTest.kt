package net.osmand.test.junit

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxDbHelper
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.util.PlatformUtil
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Since #25681 the track menu shows the summary analysis stored in the GPX database, so it has to report the
 * speed and elevation data of the track like the analysis it was stored from: the Overview blocks, the chart
 * types of Analyze on map and the 3D visualization types depend on it.
 */
@RunWith(AndroidJUnit4::class)
class GpxDatabaseSummaryTest {

	companion object {
		private const val WAIT_TIMEOUT_MS = 180_000L
		private const val TRACK_NAME = "gpx_database_summary_test.gpx"
	}

	@Before
	fun setup() {
		val app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		waitUntil("application initialization") { !app.isApplicationInitializing }
		waitUntil("GPX readers") { !GpxDbHelper.isReading() }
	}

	@Test
	fun storedSummaryKeepsSpeedAndElevationData() {
		val analysis = createGpxFile().getAnalysis(0)
		assertTrue("The track is expected to have speed data", analysis.hasSpeedData())
		assertTrue("The track is expected to have elevation data", analysis.hasElevationData())

		val file = KFile(PlatformUtil.getOsmAndContext().getGpxDir(), TRACK_NAME)
		val database = GpxDbHelper.getGPXDatabase()
		database.remove(file)
		try {
			assertTrue(database.add(GpxDataItem(file).apply { setAnalysis(analysis) }))
			val stored = database.getGpxDataItem(file)?.getAnalysis()
			assertNotNull("The track is not in the GPX database", stored)
			assertTrue("The stored summary lost the speed data", stored!!.hasSpeedData())
			assertTrue("The stored summary lost the elevation data", stored.hasElevationData())
		} finally {
			database.remove(file)
		}
	}

	private fun createGpxFile() = GpxFile(null).apply {
		tracks.add(Track().apply {
			segments.add(TrkSegment().apply {
				points.add(createPoint(50.0, 30.0, 100.0, 1_000L))
				points.add(createPoint(50.001, 30.001, 120.0, 61_000L))
				points.add(createPoint(50.002, 30.002, 110.0, 121_000L))
			})
		})
	}

	private fun createPoint(lat: Double, lon: Double, elevation: Double, time: Long) = WptPt().apply {
		this.lat = lat
		this.lon = lon
		ele = elevation
		this.time = time
	}

	private fun waitUntil(what: String, condition: () -> Boolean) {
		val deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MS
		while (!condition()) {
			assertTrue("timed out waiting for $what", SystemClock.elapsedRealtime() < deadline)
			SystemClock.sleep(100L)
		}
	}
}
