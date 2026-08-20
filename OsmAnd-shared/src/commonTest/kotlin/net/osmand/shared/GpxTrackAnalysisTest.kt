package net.osmand.shared

import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.SplitSegment
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GpxTrackAnalysisTest {

	@Test
	fun splitByDistanceKeepsWholeTrackAttributesIntact() {
		val segment = createSparseSegment()
		val totalDistanceBefore = analyze(segment).totalDistance
		val sourceAttributes = requireNotNull(segment.points[1].attributes)
		sourceAttributes.heartRate = 5f
		sourceAttributes.rawZoom = 6f
		sourceAttributes.engineLoad = 7f
		val sourceDistance = sourceAttributes.distance
		val sourceTimeDiff = sourceAttributes.timeDiff
		val sourceSpeed = sourceAttributes.speed
		val sourceFirstPoint = sourceAttributes.firstPoint
		val sourceLastPoint = sourceAttributes.lastPoint

		val analyser = GpxTrackAnalysis.TrackPointsAnalyser { _, point, attributes ->
			if (point.attributes !== attributes) {
				attributes.heartRate = 15f
				attributes.rawZoom = 16f
				attributes.engineLoad = 17f
			}
		}
		repeat(2) {
			val boundaryAttributes = segment.splitByDistance(1_000.0, false, analyser)[1].pointAttributes[1]

			assertTrue(boundaryAttributes !== sourceAttributes)
			assertTrue(boundaryAttributes.distance > 0f)
			assertTrue(boundaryAttributes.distance < sourceDistance)
			assertTrue(boundaryAttributes.timeDiff > 0f)
			assertTrue(boundaryAttributes.timeDiff < sourceTimeDiff)
			assertEquals(15f, boundaryAttributes.heartRate)
			assertEquals(16f, boundaryAttributes.rawZoom)
			assertEquals(17f, boundaryAttributes.engineLoad)
			assertSame(sourceAttributes, segment.points[1].attributes)
			assertEquals(sourceDistance, sourceAttributes.distance)
			assertEquals(sourceTimeDiff, sourceAttributes.timeDiff)
			assertEquals(sourceSpeed, sourceAttributes.speed)
			assertEquals(sourceFirstPoint, sourceAttributes.firstPoint)
			assertEquals(sourceLastPoint, sourceAttributes.lastPoint)
			assertEquals(5f, sourceAttributes.heartRate)
			assertEquals(6f, sourceAttributes.rawZoom)
			assertEquals(7f, sourceAttributes.engineLoad)
			assertEquals(totalDistanceBefore, analyze(segment).totalDistance, 0.001f)
		}
	}

	@Test
	fun exactSubsegmentStartKeepsWholeTrackAttributesIntact() {
		val segment = createSparseSegment()
		val totalDistanceBefore = analyze(segment).totalDistance
		val sourceAttributes = requireNotNull(segment.points[1].attributes)
		val sourceDistance = sourceAttributes.distance
		val sourceTimeDiff = sourceAttributes.timeDiff
		// Uphill/downhill intervals start at existing extremum points.
		val subsegment = SplitSegment(1, segment.points.size, segment)

		val startAttributes = GpxTrackAnalysis()
			.prepareInformation(0, null, subsegment)
			.pointAttributes[0]

		assertTrue(startAttributes !== sourceAttributes)
		assertEquals(0f, startAttributes.distance)
		assertEquals(0f, startAttributes.timeDiff)
		assertSame(sourceAttributes, segment.points[1].attributes)
		assertEquals(sourceDistance, sourceAttributes.distance)
		assertEquals(sourceTimeDiff, sourceAttributes.timeDiff)
		assertEquals(totalDistanceBefore, analyze(segment).totalDistance, 0.001f)
	}

	@Test
	fun splitSlopeUsesPartialBoundaryAttributes() {
		val segment = createSlowUphillSegment()
		analyze(segment)
		val sourceAttributes = requireNotNull(segment.points[1].attributes)
		val partialSegment = SplitSegment(segment, 0, 0.9).apply {
			setLastPoint(2, 1.0)
		}

		val analysis = GpxTrackAnalysis().prepareInformation(0, null, partialSegment)
		val boundaryAttributes = analysis.pointAttributes[1]

		assertTrue(boundaryAttributes.distance < sourceAttributes.distance)
		assertTrue(boundaryAttributes.timeDiff < sourceAttributes.timeDiff)
		assertEquals(0L, requireNotNull(analysis.lastUphill).movingTime)
	}

	private fun createSparseSegment() = TrkSegment().apply {
		points.add(WptPt(0.0, 0.0, 1_000_000L, Double.NaN, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.01, 1_100_000L, Double.NaN, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.02, 1_200_000L, Double.NaN, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.03, 1_300_000L, Double.NaN, 0f, Float.NaN))
	}

	private fun createSlowUphillSegment() = TrkSegment().apply {
		points.add(WptPt(0.0, 0.0, 1_000_000L, 0.0, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.01, 21_000_000L, 10.0, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.02, 41_000_000L, 20.0, 0f, Float.NaN))
		points.add(WptPt(0.0, 0.03, 61_000_000L, 30.0, 0f, Float.NaN))
	}

	private fun analyze(segment: TrkSegment): GpxTrackAnalysis {
		return GpxTrackAnalysis().prepareInformation(0, null, SplitSegment(segment))
	}
}
