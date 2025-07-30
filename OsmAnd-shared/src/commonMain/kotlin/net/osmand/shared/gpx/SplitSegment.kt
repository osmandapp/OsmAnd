package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.gpx.primitives.TrkSegment

class SplitSegment {

	var segment: TrkSegment
	var startCoeff: Double = 0.0
	var startPointInd: Int
	var endCoeff: Double = 0.0
	var endPointInd: Int = 0
	var metricEnd: Double = 0.0
	var secondaryMetricEnd: Double = 0.0
	var segmentSlopeType: TrkSegment.SegmentSlopeType? = null
	var slopeCount: Int? = null
	var slopeValue: Double? = null

	constructor(segment: TrkSegment) {
		startPointInd = 0
		startCoeff = 0.0
		endPointInd = segment.points.size - 2
		endCoeff = 1.0
		this.segment = segment
	}

	constructor(startInd: Int, endInd: Int, segment: TrkSegment) {
		startPointInd = startInd
		startCoeff = 0.0
		endPointInd = endInd - 2
		endCoeff = 1.0
		this.segment = segment
	}

	constructor(segment: TrkSegment, pointInd: Int, cf: Double) {
		this.segment = segment
		this.startPointInd = pointInd
		this.startCoeff = cf
	}

	fun getNumberOfPoints(): Int {
		return endPointInd - startPointInd + 2
	}

	operator fun get(j: Int): WptPt {
		val ind = j + startPointInd
		return when {
			j == 0 -> {
				if (startCoeff == 0.0) {
					segment.points[ind]
				} else {
					approx(segment.points[ind], segment.points[ind + 1], startCoeff)
				}
			}
			j == getNumberOfPoints() - 1 -> {
				if (endCoeff == 1.0) {
					segment.points[ind]
				} else {
					approx(segment.points[ind - 1], segment.points[ind], endCoeff)
				}
			}
			else -> segment.points[ind]
		}
	}

	private fun approx(w1: WptPt, w2: WptPt, cf: Double): WptPt {
		val time = value(w1.time, w2.time, 0L, cf)
		val speed = value(w1.speed, w2.speed, 0.0, cf)
		val ele = value(w1.ele, w2.ele, 0.0, cf)
		val hdop = value(w1.hdop, w2.hdop, 0.0, cf)
		val lat = value(w1.lat, w2.lat, -360.0, cf)
		val lon = value(w1.lon, w2.lon, -360.0, cf)
		return WptPt(lat, lon, time, ele, speed, hdop)
	}

	private fun value(vl: Double, vl2: Double, none: Double, cf: Double): Double {
		return when {
			vl == none || vl.isNaN() -> vl2
			vl2 == none || vl2.isNaN() -> vl
			else -> vl + cf * (vl2 - vl)
		}
	}

	private fun value(vl: Long, vl2: Long, none: Long, cf: Double): Long {
		return when (vl) {
			none -> vl2
			else -> when (vl2) {
				none -> vl
				else -> vl + (cf * (vl2 - vl)).toLong()
			}
		}
	}

	fun setLastPoint(pointInd: Int, endCf: Double): Double {
		endCoeff = endCf
		endPointInd = pointInd
		return endCoeff
	}
}
