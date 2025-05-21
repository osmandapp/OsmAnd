package net.osmand.shared.gpx.primitives

import net.osmand.shared.gpx.ElevationDiffsCalculator.*
import net.osmand.shared.gpx.ElevationWptApproximator
import net.osmand.shared.gpx.ElevationWptDiffsCalculator
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.SplitMetric
import net.osmand.shared.gpx.SplitSegment

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

	enum class SegmentSlopeType {
		UPHILL,
		DOWNHILL,
		FLAT
	}

	private fun TrkSegment.splitBySlopeTypeUsingExtremums(): List<SplitSegment> {
		val splitSegments = mutableListOf<SplitSegment>()
		if (points.size < 3) return emptyList()

		val extremums = getExtremums()
		if (extremums.size < 2) return emptyList()

		val accumulatedDistances = accumulatePointsDistance()

		var prevExtremumIndex: Int? = null
		var sp: SplitSegment? = null

		for(i in points.indices){
			val point = points[i]
			val isExtremum = isPointExtremum(point, extremums)

			if (i > 0) {
				if (prevExtremumIndex != null && isExtremum) {
					val prevExtremumPoint = points[prevExtremumIndex]
					val extremumElevDiff = prevExtremumPoint.ele - point.ele
					val slopeType =
						if (extremumElevDiff.toInt() == 0) SegmentSlopeType.FLAT
						else if(extremumElevDiff < 0) SegmentSlopeType.UPHILL
						else SegmentSlopeType.DOWNHILL
					if (sp != null && sp.segmentSlopeType == slopeType) {
						sp.metricEnd = accumulatedDistances[i]
						sp.endPointInd = i - 1
					} else {
						sp = SplitSegment(this)
						sp.startPointInd = prevExtremumIndex
						sp.endPointInd = i - 1
						sp.metricEnd = accumulatedDistances[i]
						sp.segmentSlopeType = slopeType
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

	private fun getExtremums(): List<Extremum>{
		val approximator = getElevationApproximator()
		if (!approximator.approximate()) return emptyList()
		val distances = approximator.getDistances() ?: return emptyList()
		val elevations = approximator.getElevations() ?: return emptyList()
		val wptPts = approximator.getApproximatedWpts()

		if (distances.size < 2) return emptyList()
		val elevationDiffsCalc = getElevationDiffsCalculator(distances, elevations, wptPts)
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

	private fun isPointExtremum(wptPt: WptPt, extremums: List<Extremum>): Boolean {
		for (extremum in extremums) {
			if (wptPt == extremum.wptPt) {
				return true
			}
		}
		return false
	}

	private fun getElevationApproximator(): ElevationWptApproximator {
		return ElevationWptApproximator(points)
	}

	private fun getElevationDiffsCalculator(
		distances: DoubleArray, elevations: DoubleArray, wptPts: MutableList<WptPt>
	): ElevationWptDiffsCalculator {
		return ElevationWptDiffsCalculator(distances, elevations, wptPts)
	}

	fun splitByUpDownHills(): List<GpxTrackAnalysis> {
		return GpxUtilities.convert(splitBySlopeTypeUsingExtremums())
	}

	fun splitByDistance(meters: Double, joinSegments: Boolean): List<GpxTrackAnalysis> {
		return split(
			SplitMetric.DistanceSplitMetric(),
			SplitMetric.TimeSplitMetric(), meters, joinSegments)
	}

	fun splitByTime(seconds: Int, joinSegments: Boolean): List<GpxTrackAnalysis> {
		return split(
			SplitMetric.TimeSplitMetric(),
			SplitMetric.DistanceSplitMetric(), seconds.toDouble(), joinSegments)
	}

	private fun split(
		metric: SplitMetric,
		secondaryMetric: SplitMetric,
		metricLimit: Double,
		joinSegments: Boolean
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
		return GpxUtilities.convert(splitSegments)
	}
}