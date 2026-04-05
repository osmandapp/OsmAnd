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
	private var materializedPoints: MutableList<WptPt>? = mutableListOf()
	private var pointSource: TrackPointSource? = null
	var points: MutableList<WptPt>
		get() = materializePoints()
		set(value) {
			materializedPoints = value
			pointSource = null
		}
	var renderer: Any? = null
	var routeSegments = mutableListOf<GpxUtilities.RouteSegment>()
	var routeTypes = mutableListOf<GpxUtilities.RouteType>()

	fun isGeneralSegment() = generalSegment

	fun hasRoute(): Boolean {
		return routeSegments.isNotEmpty() && routeTypes.isNotEmpty()
	}

	fun getPointsSize(): Int = pointSource?.size ?: materializedPoints?.size ?: 0

	fun isPointsEmpty(): Boolean = getPointsSize() == 0

	fun addPointsTo(destination: MutableList<WptPt>) {
		materializedPoints?.let {
			destination.addAll(it)
			return
		}
		for (index in 0 until getPointsSize()) {
			destination.add(getPointSnapshot(index))
		}
	}

	fun getPointSnapshot(index: Int): WptPt {
		val materialized = materializedPoints
		if (materialized != null) {
			return materialized[index]
		}
		return WptPt().also { copyPointTo(index, it) }
	}

	internal fun copyPointTo(index: Int, destination: WptPt) {
		val materialized = materializedPoints
		if (materialized != null) {
			copyPoint(materialized[index], destination)
		} else {
			pointSource!!.copyPointTo(index, destination)
		}
	}

	fun getPointLat(index: Int): Double = materializedPoints?.get(index)?.lat ?: pointSource!!.getLat(index)

	fun getPointLon(index: Int): Double = materializedPoints?.get(index)?.lon ?: pointSource!!.getLon(index)

	fun getPointTime(index: Int): Long = materializedPoints?.get(index)?.time ?: pointSource!!.getTime(index)

	fun getPointEle(index: Int): Double = materializedPoints?.get(index)?.ele ?: pointSource!!.getEle(index)

	fun getFirstPointSnapshot(): WptPt? = if (isPointsEmpty()) null else getPointSnapshot(0)

	fun getLastPointSnapshot(): WptPt? =
		if (isPointsEmpty()) null else getPointSnapshot(getPointsSize() - 1)

	fun appendParsedPoint(lat: Double, lon: Double): Int {
		return ensureMutablePointSource().addPoint(lat, lon)
	}

	fun appendParsedPoint(point: WptPt): Int {
		return ensureMutablePointSource().addPoint(point)
	}

	fun setPointTime(index: Int, value: Long) {
		ensureMutablePointSource().setTime(index, value)
	}

	fun setPointDistance(index: Int, value: Double) {
		ensureMutablePointSource().setDistance(index, value)
	}

	fun setPointElevation(index: Int, value: Double) {
		ensureMutablePointSource().setEle(index, value)
	}

	fun setPointSpeed(index: Int, value: Float) {
		ensureMutablePointSource().setSpeed(index, value)
	}

	fun setPointHdop(index: Int, value: Float) {
		ensureMutablePointSource().setHdop(index, value)
	}

	fun setPointBearing(index: Int, value: Float) {
		ensureMutablePointSource().setBearing(index, value)
	}

	fun setPointHeading(index: Int, value: Float) {
		ensureMutablePointSource().setHeading(index, value)
	}

	fun setPointName(index: Int, value: String?) {
		ensureMutablePointSource().setName(index, value)
	}

	fun setPointDesc(index: Int, value: String?) {
		ensureMutablePointSource().setDesc(index, value)
	}

	fun setPointCategory(index: Int, value: String?) {
		ensureMutablePointSource().setCategory(index, value)
	}

	fun getPointCategory(index: Int): String? {
		val materialized = materializedPoints
		return if (materialized != null) {
			materialized[index].category
		} else {
			(pointSource as? MutableTrackPointSource)?.getCategory(index)
		}
	}

	fun setPointComment(index: Int, value: String?) {
		ensureMutablePointSource().setComment(index, value)
	}

	fun setPointLink(index: Int, value: Link?) {
		ensureMutablePointSource().setLink(index, value)
	}

	fun putPointExtension(index: Int, key: String, value: String) {
		ensureMutablePointSource().putExtension(index, key, value)
	}

	fun setPointFirst(index: Int, value: Boolean) {
		ensureMutablePointSource().setFirstPoint(index, value)
	}

	fun setPointLast(index: Int, value: Boolean) {
		ensureMutablePointSource().setLastPoint(index, value)
	}

	fun setJoinedPointSources(segments: List<TrkSegment>, markInnerSegments: Boolean) {
		materializedPoints = null
		pointSource = JoinedTrackPointSource(segments.map { JoinedSegmentSource(it) }, markInnerSegments)
	}

	private fun materializePoints(): MutableList<WptPt> {
		val currentPoints = materializedPoints
		if (currentPoints != null) {
			return currentPoints
		}
		val source = pointSource ?: return mutableListOf<WptPt>().also { materializedPoints = it }
		val points = ArrayList<WptPt>(source.size)
		for (index in 0 until source.size) {
			points.add(WptPt().also { source.copyPointTo(index, it) })
		}
		materializedPoints = points
		pointSource = null
		return points
	}

	private fun ensureMutablePointSource(): MutableTrackPointSource {
		val source = pointSource
		if (source is MutableTrackPointSource) {
			return source
		}
		val mutableSource = MutableTrackPointSource(getPointsSize())
		materializedPoints?.let { points ->
			for (point in points) {
				mutableSource.addPoint(point)
			}
		} ?: source?.let {
			for (index in 0 until it.size) {
				val point = WptPt()
				it.copyPointTo(index, point)
				mutableSource.addPoint(point)
			}
		}
		materializedPoints = null
		pointSource = mutableSource
		return mutableSource
	}

	private fun copyPoint(source: WptPt, destination: WptPt) {
		destination.lat = source.lat
		destination.lon = source.lon
		destination.time = source.time
		destination.distance = source.distance
		destination.ele = source.ele
		destination.speed = source.speed
		destination.hdop = source.hdop
		destination.bearing = source.bearing
		destination.heading = source.heading
		destination.colourARGB = source.colourARGB
		destination.altitudeColor = source.altitudeColor
		destination.speedColor = source.speedColor
		destination.slopeColor = source.slopeColor
		destination.deleted = source.deleted
		destination.firstPoint = source.firstPoint
		destination.lastPoint = source.lastPoint
		destination.name = source.name
		destination.desc = source.desc
		destination.category = source.category
		destination.comment = source.comment
		destination.link = source.link?.let { Link(it) }
		destination.attributes = source.attributes
		val extensions = source.getExtensionsToRead()
		if (extensions.isNotEmpty()) {
			destination.getExtensionsToWrite().putAll(extensions)
		}
	}

	enum class SegmentSlopeType(val symbol: String) {
		UPHILL("↑"),
		DOWNHILL("↓"),
		FLAT("↔");
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
