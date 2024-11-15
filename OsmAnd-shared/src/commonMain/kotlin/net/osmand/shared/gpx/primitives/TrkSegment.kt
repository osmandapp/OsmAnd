package net.osmand.shared.gpx.primitives

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