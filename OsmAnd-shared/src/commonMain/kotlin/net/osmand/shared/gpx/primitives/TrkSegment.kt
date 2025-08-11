package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.ElevationApproximator
import net.osmand.shared.gpx.ElevationDiffsCalculator
import net.osmand.shared.gpx.ElevationDiffsCalculator.*
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.SplitMetric
import net.osmand.shared.gpx.SplitSegment
import kotlin.math.absoluteValue

class TrkSegment : GpxExtensions() {
	var name: String? = null
	var generalSegment = false
	var points = mutableListOf<WptPt>()
	var renderer: Any? = null
	var routeSegments = mutableListOf<GpxUtilities.RouteSegment>()
	var routeTypes = mutableListOf<GpxUtilities.RouteType>()

	fun isGeneralSegment() = generalSegment

	fun hasRoute(): Boolean {
		return routeSegments.isNotEmpty() && routeTypes.isNotEmpty()
	}

	enum class SegmentSlopeType(val symbol: String) {
		UPHILL("↗"),
		DOWNHILL("↘"),
		FLAT("➡");
	}

	private fun TrkSegment.splitBySlopeTypeUsingExtremums(): List<SplitSegment> {
		val splitSegments = mutableListOf<SplitSegment>()
		if (points.size < 3) return emptyList()

		val extremums = getExtremums()
		if (extremums.size < 2) return emptyList()

		val accumulatedDistances = accumulatePointsDistance()

		var prevExtremumIndex: Int? = null
		var sp: SplitSegment? = null

		val slopeCounters = mutableMapOf(
			SegmentSlopeType.UPHILL to 0,
			SegmentSlopeType.DOWNHILL to 0,
			SegmentSlopeType.FLAT to 0
		)

		for (i in points.indices) {
			val point = points[i]
			val isExtremum = isPointExtremum(i, extremums)

			if (i > 0) {
				if (prevExtremumIndex != null && isExtremum) {
					val prevExtremumPoint = points[prevExtremumIndex]
					val extremumElevDiff = prevExtremumPoint.ele - point.ele

					val slopeType =
						if (extremumElevDiff.absoluteValue < 1) SegmentSlopeType.FLAT
						else if (extremumElevDiff < 0) SegmentSlopeType.UPHILL
						else SegmentSlopeType.DOWNHILL
					if (sp != null && sp.segmentSlopeType == slopeType) {
						sp.metricEnd = accumulatedDistances[i]
						sp.endPointInd = i - 1
						sp.slopeValue = getSlopeValue(sp, point)
					} else {
						val count = (slopeCounters[slopeType] ?: 0) + 1
						slopeCounters[slopeType] = count

						sp = SplitSegment(this)
						sp.startPointInd = prevExtremumIndex
						sp.endPointInd = i - 1
						sp.metricEnd = accumulatedDistances[i]
						sp.segmentSlopeType = slopeType
						sp.slopeCount = count
						sp.slopeValue = getSlopeValue(sp, point)

						splitSegments.add(sp)
					}
				}

				if (isExtremum) {
					prevExtremumIndex = i
				}
			}
			if (isExtremum) {
				prevExtremumIndex = i
			}
		}

		return splitSegments
	}

	private fun getSlopeValue(sp: SplitSegment, point: WptPt): Double {
		val startPoint = points[sp.startPointInd]
		if (startPoint.ele.isNaN() || point.ele.isNaN()) {
			return 0.0
		}
		val distance = point.distance - startPoint.distance
		if (distance == 0.0) {
			return 0.0
		}
		val elevationDiff = point.ele - startPoint.ele
		return elevationDiff / distance * 100
	}

	private fun getExtremums(): List<Extremum>{
		val approximator = getElevationApproximator()
		if (!approximator.approximate()) return emptyList()
		val distances = approximator.getDistances() ?: return emptyList()
		val elevations = approximator.getElevations() ?: return emptyList()
		val survivedIndexes = approximator.getSurvivedIndexes() ?: return emptyList()

		if (distances.size < 2) return emptyList()
		val elevationDiffsCalc = getElevationDiffsCalculator(distances, elevations, survivedIndexes)
		elevationDiffsCalc.calculateElevationDiffs()
		val extremums = elevationDiffsCalc.getExtremums()

		return extremums
	}

	private fun accumulatePointsDistance(): DoubleArray{
		val metric: SplitMetric = SplitMetric.DistanceSplitMetric()
		val accumulatedDistances = DoubleArray(points.size)
		var prev: WptPt? = null
		var totalDist = 0.0

		for (i in points.indices) {
			val point = points[i]
			if (i > 0) {
				prev?.let {
					val calcDist = metric.metric(it, point)
					totalDist += calcDist
					accumulatedDistances[i] = totalDist
				}
			}
			prev = point
		}
		return accumulatedDistances
	}

	private fun isPointExtremum(index: Int, extremums: List<Extremum>): Boolean {
		for (extremum in extremums) {
			if (index == extremum.index) {
				return true
			}
		}
		return false
	}

	private fun getElevationApproximator(): ElevationApproximator {
		return object : ElevationApproximator() {
			override fun getPointLatitude(index: Int): Double {
				return points[index].lat
			}

			override fun getPointLongitude(index: Int): Double {
				return points[index].lon
			}

			override fun getPointElevation(index: Int): Double {
				return points[index].ele
			}

			override fun getPointsCount(): Int {
				return points.size
			}
		}
	}

	private fun getElevationDiffsCalculator(
		distances: DoubleArray, elevations: DoubleArray, indexes: IntArray
	): ElevationDiffsCalculator {
		return object : ElevationDiffsCalculator() {
			override fun getPointDistance(index: Int): Double {
				return distances[index]
			}

			override fun getPointIndex(index: Int): Int {
				return indexes[index]
			}

			override fun getPointElevation(index: Int): Double {
				return elevations[index]
			}

			override fun getPointsCount(): Int {
				return distances.size
			}
		}
	}

	fun splitByUpDownHills(): List<GpxTrackAnalysis> {
		return splitByUpDownHills(null)
	}

	fun splitByUpDownHills(pointsAnalyser: TrackPointsAnalyser?): List<GpxTrackAnalysis> {
		return GpxUtilities.convert(splitBySlopeTypeUsingExtremums(), pointsAnalyser)
	}

	fun splitByDistance(meters: Double, joinSegments: Boolean): List<GpxTrackAnalysis> {
		return splitByDistance(meters, joinSegments, null)
	}

	fun splitByDistance(
		meters: Double,
		joinSegments: Boolean,
		pointsAnalyser: TrackPointsAnalyser?
	): List<GpxTrackAnalysis> {
		return split(
			SplitMetric.DistanceSplitMetric(),
			SplitMetric.TimeSplitMetric(), meters, joinSegments, pointsAnalyser
		)
	}

	fun splitByTime(seconds: Int, joinSegments: Boolean): List<GpxTrackAnalysis> {
		return splitByTime(seconds, joinSegments, null)
	}

	fun splitByTime(
		seconds: Int,
		joinSegments: Boolean,
		pointsAnalyser: TrackPointsAnalyser?
	): List<GpxTrackAnalysis> {
		return split(
			SplitMetric.TimeSplitMetric(),
			SplitMetric.DistanceSplitMetric(), seconds.toDouble(), joinSegments, pointsAnalyser
		)
	}

	private fun split(
		metric: SplitMetric,
		secondaryMetric: SplitMetric,
		metricLimit: Double,
		joinSegments: Boolean,
		pointsAnalyser: TrackPointsAnalyser?
	): List<GpxTrackAnalysis> {
		val splitSegments = mutableListOf<SplitSegment>()
		SplitMetric.splitSegment(
			metric,
			secondaryMetric,
			metricLimit,
			splitSegments,
			this,
			joinSegments
		)
		return GpxUtilities.convert(splitSegments, pointsAnalyser)
	}
}