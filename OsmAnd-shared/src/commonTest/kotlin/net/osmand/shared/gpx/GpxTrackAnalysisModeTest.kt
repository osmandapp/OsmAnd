package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpxTrackAnalysisModeTest {

	@Test
	fun summaryAnalysisPreservesAggregatesWithoutCollectingPointAttributes() {
		val segment = TrkSegment().apply {
			points.add(createPoint(50.0, 30.0, 100.0, 1_000L))
			points.add(createPoint(50.001, 30.001, 120.0, 2_000L))
			points.add(createPoint(50.002, 30.002, 110.0, 3_000L))
		}
		val gpxFile = GpxFile(null).apply {
			tracks.add(Track().apply { segments.add(segment) })
		}

		val summary = gpxFile.getAnalysis(10L, null, null, null, false)
		val full = gpxFile.getAnalysis(10L, null, null, null, true)

		assertFalse(summary.collectPointData)
		assertTrue(summary.pointAttributes.isEmpty())
		assertTrue(full.collectPointData)
		assertEquals(full.points, summary.points)
		assertEquals(full.totalDistance, summary.totalDistance)
		assertEquals(full.minElevation, summary.minElevation)
		assertEquals(full.maxElevation, summary.maxElevation)
		assertEquals(full.timeSpan, summary.timeSpan)
	}

	@Test
	fun databaseSummaryExposesElevationMetricsWithoutPointData() {
		val summary = GpxTrackAnalysis().apply {
			collectPointData = false
			minElevation = 168.0
			maxElevation = 425.0
			avgElevation = 286.0
			diffElevationUp = 1904.0
			diffElevationDown = 1679.0
		}

		assertFalse(summary.hasElevationData())
		assertTrue(summary.hasElevationMetrics())
	}

	private fun createPoint(lat: Double, lon: Double, elevation: Double, time: Long) =
		WptPt().apply {
			this.lat = lat
			this.lon = lon
			ele = elevation
			this.time = time
		}
}
