package net.osmand.shared

import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.shared.gpx.PointAttributes
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
	fun testLoadAndWritePointsGroupsXmlPreservesGroupAppearance() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test" xmlns:osmand="https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx">
			  <wpt lat="10.0" lon="20.0">
			    <name>Cafe</name>
			    <type>Food</type>
			  </wpt>
			  <extensions>
			    <osmand:points_groups>
			      <group name="Food" color="#ff0000" icon="restaurant" background="circle" hidden="true" pinned="false" />
			    </osmand:points_groups>
			  </extensions>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)

		assertNull(gpxFile.error)
		val pointsGroup = assertNotNull(gpxFile.pointsGroups["Food"])
		assertEquals("restaurant", pointsGroup.iconName)
		assertEquals("circle", pointsGroup.backgroundType)
		assertTrue(pointsGroup.isHidden())
		assertEquals(false, pointsGroup.isPinned())
		assertEquals(1, pointsGroup.points.size)

		val written = writeGpxToString(gpxFile)
		assertTrue(written.contains("points_groups"))
		assertTrue(written.contains("Food"))
		assertTrue(written.contains("restaurant"))
		assertTrue(written.contains("pinned"))
	}

	@Test
	fun testStoredPointsGroupsOverrideParsedXmlWhenApplied() {
		val gpxFile = loadGpx(
			"""
			<gpx version="1.1" creator="test">
			  <wpt lat="10.0" lon="20.0">
			    <name>Cafe</name>
			    <type>Food</type>
			  </wpt>
			  <extensions>
			    <points_groups>
			      <group name="Food" icon="restaurant" background="circle" hidden="true" />
			    </points_groups>
			  </extensions>
			</gpx>
			""".trimIndent(),
			addGeneralTrack = false
		)
		assertNull(gpxFile.error)
		assertEquals("restaurant", assertNotNull(gpxFile.pointsGroups["Food"]).iconName)

		val storedPointsGroups = GpxUtilities.serializePointsGroups(
			mapOf("Food" to PointsGroup("Food", "bar", "square", 0, hidden = false, pinned = true))
		)
		GpxUtilities.applyPointsGroups(gpxFile, storedPointsGroups)

		val pointsGroup = assertNotNull(gpxFile.pointsGroups["Food"])
		assertEquals("bar", pointsGroup.iconName)
		assertEquals("square", pointsGroup.backgroundType)
		assertEquals(true, pointsGroup.isPinned())
		assertEquals(1, pointsGroup.points.size)
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
