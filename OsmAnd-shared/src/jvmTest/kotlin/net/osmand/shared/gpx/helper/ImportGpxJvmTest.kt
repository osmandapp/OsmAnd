package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.WptPt
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportGpxJvmTest {

	@Test
	fun testCoordinatesSeparatedByNewlinesAndTabsFormOneTrack() {
		val coordinates = "\n\t\t30.0,50.0,10\n\t\t31.0,51.0,20\n32.0,52.0,30\t33.0,53.0,40\n"
		val gpxFile = importKml(
			"<Placemark><name>Line</name><LineString><coordinates>$coordinates</coordinates></LineString></Placemark>"
		)

		assertNull(gpxFile.error)
		assertTrue(gpxFile.isPointsEmpty())
		val points = singleTrackPoints(gpxFile, "Line")
		assertEquals(listOf(50.0, 51.0, 52.0, 53.0), points.map { it.lat })
		assertEquals(listOf(30.0, 31.0, 32.0, 33.0), points.map { it.lon })
		assertEquals(listOf(10.0, 20.0, 30.0, 40.0), points.map { it.ele })
	}

	@Test
	fun testGxCoordsSeparatedByRepeatedWhitespaceKeepAllValues() {
		val gpxFile = importKml(
			"<Placemark><name>Walk</name><gx:Track>" +
					"<when>2026-05-01T07:00:00Z</when><when>2026-05-01T07:00:01Z</when>" +
					"<gx:coord>18.1  42.6 10</gx:coord><gx:coord>18.2\t42.7\t\t11</gx:coord>" +
					"</gx:Track></Placemark>"
		)

		assertNull(gpxFile.error)
		val points = singleTrackPoints(gpxFile, "Walk")
		assertEquals(listOf(42.6, 42.7), points.map { it.lat })
		assertEquals(listOf(18.1, 18.2), points.map { it.lon })
		assertEquals(listOf(10.0, 11.0), points.map { it.ele })
		assertEquals(1_000L, points[1].time - points[0].time)
	}

	@Test
	fun testSinglePointPlacemarkBecomesWaypointOfItsFolder() {
		val gpxFile = importKml(
			"<Folder><name>Stops</name><Placemark><name>Stop</name>" +
					"<Point><coordinates>\n\t17.97,42.88,5\n</coordinates></Point></Placemark></Folder>"
		)

		assertNull(gpxFile.error)
		assertEquals(0, gpxFile.getTracksCount())
		val point = gpxFile.getPointsList().single()
		assertEquals("Stop", point.name)
		assertEquals("Stops", point.category)
		assertEquals(42.88, point.lat)
		assertEquals(17.97, point.lon)
	}

	private fun importKml(body: String): GpxFile =
		ImportGpx.loadGPXFileFromKml(
			Buffer().writeUtf8(
				"<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" +
						"<Document><name>Doc</name>$body</Document></kml>"
			)
		).first

	private fun singleTrackPoints(gpxFile: GpxFile, name: String): List<WptPt> {
		val track = gpxFile.tracks.single { !it.generalTrack }
		assertEquals(name, track.name)
		return track.segments.single().points
	}
}
