package net.osmand.shared.gpx

import kotlin.math.*
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.extensions.toRadians

abstract class SplitMetric {

	abstract fun metric(p1: WptPt, p2: WptPt): Double

	class DistanceSplitMetric : SplitMetric() {
		private val calculations = FloatArray(1)

		override fun metric(p1: WptPt, p2: WptPt): Double {
			// Replace the net.osmand.Location.distanceBetween method with an appropriate implementation
			// Here is a simple haversine formula for demonstration purposes
			val R = 6371e3 // Radius of the earth in meters
			val lat1 = p1.lat.toRadians()
			val lat2 = p2.lat.toRadians()
			val dLat = (p2.lat - p1.lat).toRadians()
			val dLon = (p2.lon - p1.lon).toRadians()

			val a = sin(dLat / 2) * sin(dLat / 2) +
					cos(lat1) * cos(lat2) *
					sin(dLon / 2) * sin(dLon / 2)
			val c = 2 * atan2(sqrt(a), sqrt(1 - a))

			return R * c
		}
	}

	class TimeSplitMetric : SplitMetric() {
		override fun metric(p1: WptPt, p2: WptPt): Double {
			return if (p1.time != 0L && p2.time != 0L) {
				abs((p2.time - p1.time) / 1000L).toDouble()
			} else {
				0.0
			}
		}
	}

	companion object {
		fun splitSegment(
			metric: SplitMetric, secondaryMetric: SplitMetric, metricLimit: Double,
			splitSegments: MutableList<SplitSegment>, segment: TrkSegment, joinSegments: Boolean
		) {
			var currentMetricEnd = metricLimit
			var secondaryMetricEnd = 0.0
			var sp = SplitSegment(segment, 0, 0.0)
			var total = 0.0
			var prev: WptPt? = null

			for (k in segment.points.indices) {
				val point = segment.points[k]
				if (k > 0) {
					var currentSegment = 0.0
					if (!(segment.generalSegment && !joinSegments && point.firstPoint)) {
						prev?.let {
							currentSegment = metric.metric(it, point)
							secondaryMetricEnd += secondaryMetric.metric(it, point)
						}
					}
					while (total + currentSegment > currentMetricEnd) {
						val p = currentMetricEnd - total
						val cf = p / currentSegment
						sp.setLastPoint(k - 1, cf)
						sp.metricEnd = currentMetricEnd
						sp.secondaryMetricEnd = secondaryMetricEnd
						splitSegments.add(sp)

						sp = SplitSegment(segment, k - 1, cf)
						currentMetricEnd += metricLimit
					}
					total += currentSegment
				}
				prev = point
			}
			if (segment.points.isNotEmpty() && !(sp.endPointInd == segment.points.size - 1 && sp.startCoeff == 1.0)) {
				sp.metricEnd = total
				sp.secondaryMetricEnd = secondaryMetricEnd
				sp.setLastPoint(segment.points.size - 2, 1.0)
				splitSegments.add(sp)
			}
		}
	}
}
