package net.osmand.shared

import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.gpx.PointAttributes
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GpxUtilitiesLoadTest {

	@Test
	fun testLoadGpxFileWithManyTimedPointsUsesLastPointAsCreationTime() {
		val pointsCount = 4096
		val startTime = 1_704_067_200_000L
		val gpxFile = loadGpx(buildTimedTrackGpx(pointsCount, startTime), addGeneralTrack = false)

		assertNull(gpxFile.error)
		assertEquals(1, gpxFile.tracks.size)
		assertEquals(1, gpxFile.getNonEmptySegmentsCount())
		val points = gpxFile.tracks[0].segments[0].points
		assertEquals(pointsCount, points.size)
		assertEquals(startTime + (pointsCount - 1) * 1_000L, points.last().time)
		assertEquals(points.last().time, gpxFile.metadata.time)
	}

	@Test
	fun testLoadGpxFileReadsPointExtensionsWithoutLosingValues() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
			  <trk>
			    <trkseg>
			      <trkpt lat="10.0" lon="20.0">
			        <time>2024-01-01T00:00:00.123Z</time>
			        <extensions>
			          <speed>5.5</speed>
			          <bearing>42.0</bearing>
			          <gpxtpx:TrackPointExtension>
			            <gpxtpx:hr>145</gpxtpx:hr>
			            <gpxtpx:cad>91</gpxtpx:cad>
			          </gpxtpx:TrackPointExtension>
			        </extensions>
			      </trkpt>
			    </trkseg>
			  </trk>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)

		assertNull(gpxFile.error)
		val point = gpxFile.tracks[0].segments[0].points[0]
		assertEquals(5.5f, point.speed)
		assertEquals(42.0f, point.bearing)
		assertEquals("145", point.getExtensionsToRead()[PointAttributes.SENSOR_TAG_HEART_RATE])
		assertEquals("91", point.getExtensionsToRead()[PointAttributes.SENSOR_TAG_CADENCE])

		val written = writeGpxToString(gpxFile)
		assertTrue(written.contains("5.5"))
		assertTrue(written.contains("bearing"))
		assertTrue(written.contains("145"))
		assertTrue(written.contains("91"))
	}

	@Test
	fun testLoadGpxFilePreservesCustomExtensionPrefix() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test" xmlns:test="https://example.com/gpx/test">
			  <wpt lat="10.0" lon="20.0">
			    <extensions>
			      <test:country>United States</test:country>
			      <test:state>Virginia</test:state>
			      <test:telephone>+1 804 828 0100</test:telephone>
			      <test:postcode>23284</test:postcode>
			      <test:start_date>1838</test:start_date>
			    </extensions>
			  </wpt>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)

		assertNull(gpxFile.error)
		val expected = mapOf(
			"test:country" to "United States",
			"test:state" to "Virginia",
			"test:telephone" to "+1 804 828 0100",
			"test:postcode" to "23284",
			"test:start_date" to "1838"
		)
		val extensions = gpxFile.getPointsList().single().getExtensionsToRead()
		assertEquals(expected, extensions.filterKeys { it.startsWith("test:") })
		assertNull(extensions["country"])
		assertNull(extensions["telephone"])

		val reloaded = loadGpx(writeGpxToString(gpxFile), addGeneralTrack = false)
		assertNull(reloaded.error)
		val reloadedExtensions = reloaded.getPointsList().single().getExtensionsToRead()
		assertEquals(expected, reloadedExtensions.filterKeys { it.startsWith("test:") })
	}

	@Test
	fun testLoadGpxFileResolvesKnownNamespacesBoundToAnotherPrefix() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test"
			     xmlns:ns1="https://osmand.net"
			     xmlns:ns3="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
			     xmlns:ns2="http://www.garmin.com/xmlschemas/GpxExtensions/v3"
			     xmlns:test="https://example.com/gpx/test">
			  <trk>
			    <trkseg>
			      <trkpt lat="10.0" lon="20.0">
			        <extensions>
			          <ns1:width>bold</ns1:width>
			          <ns2:DisplayColor>Red</ns2:DisplayColor>
			          <ns3:TrackPointExtension>
			            <ns3:hr>145</ns3:hr>
			            <ns3:cad>91</ns3:cad>
			          </ns3:TrackPointExtension>
			          <test:hr>1</test:hr>
			        </extensions>
			      </trkpt>
			    </trkseg>
			  </trk>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)

		assertNull(gpxFile.error)
		val point = gpxFile.tracks[0].segments[0].points[0]
		val extensions = point.getExtensionsToRead()
		assertEquals("145", extensions[PointAttributes.SENSOR_TAG_HEART_RATE])
		assertEquals("91", extensions[PointAttributes.SENSOR_TAG_CADENCE])
		assertEquals("bold", extensions[GpxUtilities.LINE_WIDTH_EXTENSION])
		assertEquals("Red", extensions["displaycolor"])
		assertEquals(GpxUtilities.parseColor("Red", null), point.getColor(null as Int?))
		assertEquals("1", extensions["test:hr"])
	}

	@Test
	fun testLoadGpxFilePreservesGeneralTrackBehaviorForMultipleSegments() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test">
			  <trk>
			    <trkseg>
			      <trkpt lat="10.0" lon="20.0"><time>2024-01-01T00:00:00Z</time></trkpt>
			      <trkpt lat="10.1" lon="20.1"><time>2024-01-01T00:00:01Z</time></trkpt>
			    </trkseg>
			    <trkseg>
			      <trkpt lat="10.2" lon="20.2"><time>2024-01-01T00:00:02Z</time></trkpt>
			      <trkpt lat="10.3" lon="20.3"><time>2024-01-01T00:00:03Z</time></trkpt>
			    </trkseg>
			  </trk>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = true
		)

		assertNull(gpxFile.error)
		assertEquals(1, gpxFile.getTracksCount())
		assertEquals(2, gpxFile.tracks.size)
		val generalTrack = assertNotNull(gpxFile.getGeneralTrack())
		val generalSegment = assertNotNull(generalTrack.segments.singleOrNull())
		assertEquals(4, generalSegment.points.size)
		assertTrue(generalSegment.points[0].firstPoint)
		assertTrue(generalSegment.points[1].lastPoint)
		assertTrue(generalSegment.points[2].firstPoint)
		assertTrue(generalSegment.points[3].lastPoint)
	}

	@Test
	fun testSpeedIsNotDuplicatedIntoPointExtensions() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test">
			  <trk>
			    <trkseg>
			      <trkpt lat="10.0" lon="20.0">
			        <extensions><speed>5.5</speed><bearing>42.0</bearing></extensions>
			      </trkpt>
			      <trkpt lat="10.1" lon="20.1"><speed>7.25</speed></trkpt>
			    </trkseg>
			  </trk>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)

		assertNull(gpxFile.error)
		val points = gpxFile.tracks[0].segments[0].points
		assertEquals(5.5f, points[0].speed)
		assertEquals(42.0f, points[0].bearing)
		assertEquals(7.25f, points[1].speed)
		// the value is kept in the field only - a string copy per point costs a map per point
		assertFalse(points[0].getExtensionsToRead().containsKey(GpxUtilities.POINT_SPEED))
		// bearing keeps its own string: the writer has no field to regenerate it from
		assertEquals("42.0", points[0].getExtensionsToRead()[GpxUtilities.POINT_BEARING])
		assertNull(points[1].extensions)

		val saved = writeGpxToString(gpxFile)
		// saving must not attach the extensions map, the deferred map or the writers back to a point
		assertNull(points[1].extensions)
		assertEquals(1, points[0].getExtensionsToRead().size)
		assertNull(points[1].deferredExtensions)
		assertTrue(points[0].extensionsWriters.isNullOrEmpty())

		val reloaded = loadGpx(saved, addGeneralTrack = false)
		val reloadedPoints = reloaded.tracks[0].segments[0].points
		assertEquals(5.5f, reloadedPoints[0].speed)
		assertEquals(42.0f, reloadedPoints[0].bearing)
		assertEquals(7.2f, reloadedPoints[1].speed) // the writer has always reformatted speed as #.#
	}

	@Test
	fun testRepeatedExtensionValuesAreSharedInsideAFile() {
		val points = 64
		val gpx = loadGpx(
			buildString {
				append("<gpx version=\"1.1\" creator=\"test\"><trk><trkseg>")
				for (index in 0 until points) {
					append("<trkpt lat=\"10.0\" lon=\"20.0\"><extensions>")
					append("<provider>gps</provider><vm_bvol>12.4</vm_bvol>")
					append("</extensions></trkpt>")
				}
				append("</trkseg></trk></gpx>")
			},
			addGeneralTrack = false
		)

		assertNull(gpx.error)
		val loaded = gpx.tracks[0].segments[0].points
		assertEquals(points, loaded.size)
		val first = loaded.first().getExtensionsToRead()
		for (point in loaded) {
			val extensions = point.getExtensionsToRead()
			assertEquals("gps", extensions["provider"])
			// one instance of every distinct value and of every tag name for the whole file
			assertSame(first["provider"], extensions["provider"])
			assertSame(first["vm_bvol"], extensions["vm_bvol"])
			assertSame(first.keys.first(), extensions.keys.first())
		}
	}

	@Test
	fun testRecordedPluginValuesSurviveASave() {
		// what SavingTrackHelper does with the values the plugins attach to a recorded point
		val point = WptPt()
		point.lat = 10.0
		point.lon = 20.0
		point.speed = 5.5f
		point.heading = 42f
		GpxUtilities.assignExtensionWriter(
			point, mapOf("vm_rpm" to "2400", PointAttributes.SENSOR_TAG_HEART_RATE to "145"), "plugins")

		val segment = TrkSegment()
		segment.points.add(point)
		val track = Track()
		track.segments.add(segment)
		val gpxFile = GpxFile("test")
		gpxFile.tracks.add(track)

		val saved = writeGpxToString(gpxFile)
		assertTrue(saved.contains("<osmand:vm_rpm>2400</osmand:vm_rpm>"), saved)
		assertTrue(saved.contains("<gpxtpx:hr>145</gpxtpx:hr>"), saved)
		assertTrue(saved.contains("<osmand:speed>5.5</osmand:speed>"), saved)
		assertTrue(saved.contains("<osmand:heading>42.0</osmand:heading>"), saved)

		// the plugin writer and its values stay on the point, the analysers read them from there
		assertEquals("2400", point.getDeferredExtensionsToRead()["vm_rpm"])
		assertEquals("145", point.getDeferredExtensionsToRead()[PointAttributes.SENSOR_TAG_HEART_RATE])
		assertNotNull(point.getExtensionsWriter("plugins"))
		assertNull(point.extensions)

		// and saving twice writes the same file
		assertEquals(saved, writeGpxToString(gpxFile))
	}

	@Test
	fun testSpeedFieldWinsOverTheStringKeptInTheMap() {
		// a non-positive speed stays in the map as a string; a value set on the field afterwards
		// is still the one that gets written, as it was when the fields were pushed into the map
		val gpxFile = loadGpx(
			"<gpx version=\"1.1\" creator=\"test\"><trk><trkseg>"
					+ "<trkpt lat=\"10.0\" lon=\"20.0\"><extensions><speed>0</speed></extensions></trkpt>"
					+ "</trkseg></trk></gpx>",
			addGeneralTrack = false
		)
		assertNull(gpxFile.error)
		val point = gpxFile.tracks[0].segments[0].points[0]
		assertEquals("0", point.getExtensionsToRead()[GpxUtilities.POINT_SPEED])
		point.speed = 5.5f

		val saved = writeGpxToString(gpxFile)
		assertTrue(saved.contains("<osmand:speed>5.5</osmand:speed>"), saved)
		assertFalse(saved.contains("<osmand:speed>0</osmand:speed>"), saved)
	}

	private fun buildTimedTrackGpx(pointsCount: Int, startTime: Long): String {
		return buildString {
			append("<gpx version=\"1.1\" creator=\"test\"><trk><trkseg>")
			for (index in 0 until pointsCount) {
				append("<trkpt lat=\"")
				append(10.0 + index * 0.0001)
				append("\" lon=\"")
				append(20.0 + index * 0.0001)
				append("\"><time>")
				append(GpxUtilities.formatTime(startTime + index * 1_000L))
				append("</time></trkpt>")
			}
			append("</trkseg></trk></gpx>")
		}
	}

	private fun loadGpx(xml: String, addGeneralTrack: Boolean) =
		GpxUtilities.loadGpxFile(null, Buffer().writeUtf8(xml), null, addGeneralTrack)

	private fun writeGpxToString(gpxFile: net.osmand.shared.gpx.GpxFile): String {
		val buffer = Buffer()
		assertNull(GpxUtilities.writeGpx(null, buffer, gpxFile, null))
		return buffer.readUtf8()
	}
}
