package net.osmand.aidl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarTestUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GpxPointsPageTest {

	@Test
	fun pageIsCappedAndTotalIsTheWholeTrack() {
		val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		val file = app.getAppPath(IndexConstants.GPX_INDEX_DIR + "test_gpx_points_page.gpx")
		val points = (0 until 2500).joinToString("") { "<trkpt lat=\"50.${10000 + it}\" lon=\"4.35\"/>" }
		file.writeText("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><trkseg>$points</trkseg></trk></gpx>")
		try {
			val page = app.aidlApi.getGpxPoints(file.name, 0, 3000)!!
			assertEquals(2500, page.total)
			assertEquals(2000, page.points.size)
		} finally {
			file.delete()
		}
	}
}
