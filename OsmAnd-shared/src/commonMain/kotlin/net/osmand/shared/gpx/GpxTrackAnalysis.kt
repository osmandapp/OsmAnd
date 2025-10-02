package net.osmand.shared.gpx

import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxUtilities.POINT_ELEVATION
import net.osmand.shared.gpx.GpxUtilities.POINT_SPEED
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.routing.RouteColorize.ColorizationType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils

class GpxTrackAnalysis {

	companion object {
		const val ANALYSIS_VERSION = 1

		fun prepareInformation(fileTimeStamp: Long,
		                       joinSegments: Boolean,
		                       pointsAnalyzer: TrackPointsAnalyser,
		                       segment: TrkSegment): GpxTrackAnalysis {
			val analysis = GpxTrackAnalysis()
			analysis.joinSegments = joinSegments
			return analysis.prepareInformation(fileTimeStamp, pointsAnalyzer, SplitSegment(segment))
		}
	}

	var name: String? = null
	var totalDistanceWithoutGaps = 0f
	var timeSpanWithoutGaps: Long = 0
	var timeMovingWithoutGaps: Long = 0
	var totalDistanceMovingWithoutGaps = 0f

	private val parameters = mutableMapOf<GpxParameter, Any?>()

	var minHdop = Double.NaN
	var maxHdop = Double.NaN

	var metricEnd = 0.0
	var secondaryMetricEnd = 0.0

	var locationStart: WptPt? = null
	var locationEnd: WptPt? = null

	var left = 0.0
	var right = 0.0
	var top = 0.0
	var bottom = 0.0

	var segmentSlopeType: TrkSegment.SegmentSlopeType? = null
	var slopeCount: Int? = null
	var slopeValue: Double? = null

	var pointAttributes = mutableListOf<PointAttributes>()
	var availableAttributes = mutableSetOf<String>()

	var maxDistanceBetweenPoints = 0.0F

	var hasSpeedInTrack = false
	var hasElevationMetricsInGpx = false

	fun getGpxParameter(parameter: GpxParameter): Any? {
		return parameters[parameter] ?: parameter.defaultValue
	}

	fun setGpxParameter(parameter: GpxParameter, value: Any?) {
		parameters[parameter] = value
	}

	fun setGpxParameters(parameters: Map<GpxParameter, Any?>) {
		this.parameters.putAll(parameters)
	}

	var startTime: Long
		get() = getGpxParameter(GpxParameter.START_TIME) as Long
		set(value) = setGpxParameter(GpxParameter.START_TIME, value)

	var endTime: Long
		get() = getGpxParameter(GpxParameter.END_TIME) as Long
		set(value) = setGpxParameter(GpxParameter.END_TIME, value)

	var timeSpan: Long
		get() = getGpxParameter(GpxParameter.TIME_SPAN) as Long
		set(value) = setGpxParameter(GpxParameter.TIME_SPAN, value)

	var expectedRouteDuration: Long
		get() = getGpxParameter(GpxParameter.EXPECTED_DURATION) as Long
		set(value) = setGpxParameter(GpxParameter.EXPECTED_DURATION, value)

	var timeMoving: Long
		get() = getGpxParameter(GpxParameter.TIME_MOVING) as Long
		set(value) = setGpxParameter(GpxParameter.TIME_MOVING, value)

	var maxElevation: Double
		get() = getGpxParameter(GpxParameter.MAX_ELEVATION) as Double
		set(value) = setGpxParameter(GpxParameter.MAX_ELEVATION, value)

	var diffElevationUp: Double
		get() = getGpxParameter(GpxParameter.DIFF_ELEVATION_UP) as Double
		set(value) = setGpxParameter(GpxParameter.DIFF_ELEVATION_UP, value)

	var diffElevationDown: Double
		get() = getGpxParameter(GpxParameter.DIFF_ELEVATION_DOWN) as Double
		set(value) = setGpxParameter(GpxParameter.DIFF_ELEVATION_DOWN, value)

	var minElevation: Double
		get() = getGpxParameter(GpxParameter.MIN_ELEVATION) as Double
		set(value) = setGpxParameter(GpxParameter.MIN_ELEVATION, value)

	var avgElevation: Double
		get() = getGpxParameter(GpxParameter.AVG_ELEVATION) as Double
		set(value) = setGpxParameter(GpxParameter.AVG_ELEVATION, value)

	var avgSpeed: Float
		get() = (getGpxParameter(GpxParameter.AVG_SPEED) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SPEED, value.toDouble())

	var minSpeed: Float
		get() = (getGpxParameter(GpxParameter.MIN_SPEED) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.MIN_SPEED, value.toDouble())

	var maxSpeed: Float
		get() = (getGpxParameter(GpxParameter.MAX_SPEED) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.MAX_SPEED, value.toDouble())

	var maxSensorHr: Int
		get() = getGpxParameter(GpxParameter.MAX_SENSOR_HEART_RATE) as Int
		set(value) = setGpxParameter(GpxParameter.MAX_SENSOR_HEART_RATE, value)

	var minSensorHr: Int
		get() = getGpxParameter(GpxParameter.MIN_SENSOR_HEART_RATE) as Int
		set(value) = setGpxParameter(GpxParameter.MIN_SENSOR_HEART_RATE, value)

	var points: Int
		get() = getGpxParameter(GpxParameter.POINTS) as Int
		set(value) = setGpxParameter(GpxParameter.POINTS, value)

	var wptPoints: Int
		get() = getGpxParameter(GpxParameter.WPT_POINTS) as Int
		set(value) = setGpxParameter(GpxParameter.WPT_POINTS, value)

	var maxSensorTemperature: Int
		get() = getGpxParameter(GpxParameter.MAX_SENSOR_TEMPERATURE) as Int
		set(value) = setGpxParameter(GpxParameter.MAX_SENSOR_TEMPERATURE, value)

	var maxSensorPower: Int
		get() = getGpxParameter(GpxParameter.MAX_SENSOR_POWER) as Int
		set(value) = setGpxParameter(GpxParameter.MAX_SENSOR_POWER, value)

	var totalTracks: Int
		get() = getGpxParameter(GpxParameter.TOTAL_TRACKS) as Int
		set(value) = setGpxParameter(GpxParameter.TOTAL_TRACKS, value)

	var maxSensorSpeed: Float
		get() = (getGpxParameter(GpxParameter.MAX_SENSOR_SPEED) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.MAX_SENSOR_SPEED, value.toDouble())

	var maxSensorCadence: Float
		get() = (getGpxParameter(GpxParameter.MAX_SENSOR_CADENCE) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.MAX_SENSOR_CADENCE, value.toDouble())

	var avgSensorSpeed: Float
		get() = (getGpxParameter(GpxParameter.AVG_SENSOR_SPEED) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SENSOR_SPEED, value.toDouble())

	var avgSensorCadence: Float
		get() = (getGpxParameter(GpxParameter.AVG_SENSOR_CADENCE) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SENSOR_CADENCE, value.toDouble())

	var avgSensorHr: Float
		get() = (getGpxParameter(GpxParameter.AVG_SENSOR_HEART_RATE) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SENSOR_HEART_RATE, value.toDouble())

	var avgSensorPower: Float
		get() = (getGpxParameter(GpxParameter.AVG_SENSOR_POWER) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SENSOR_POWER, value.toDouble())

	var avgSensorTemperature: Float
		get() = (getGpxParameter(GpxParameter.AVG_SENSOR_TEMPERATURE) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.AVG_SENSOR_TEMPERATURE, value.toDouble())

	var totalDistanceMoving: Float
		get() = (getGpxParameter(GpxParameter.TOTAL_DISTANCE_MOVING) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.TOTAL_DISTANCE_MOVING, value.toDouble())

	var totalDistance: Float
		get() = (getGpxParameter(GpxParameter.TOTAL_DISTANCE) as Double).toFloat()
		set(value) = setGpxParameter(GpxParameter.TOTAL_DISTANCE, value.toDouble())

	var joinSegments: Boolean
		get() = (getGpxParameter(GpxParameter.JOIN_SEGMENTS) as Boolean)
		set(value) = (setGpxParameter(GpxParameter.JOIN_SEGMENTS, value))

	fun isTimeSpecified(): Boolean {
		val startTime = startTime
		val endTime = endTime
		return startTime != Long.MAX_VALUE && startTime != 0L && endTime != Long.MIN_VALUE && endTime != 0L
	}

	fun isTimeMoving(): Boolean {
		return timeMoving != 0L
	}

	fun isElevationSpecified(): Boolean {
		return maxElevation != -100.0
	}

	fun hasSpeedInTrack(): Boolean {
		return hasSpeedInTrack
	}

	fun isBoundsCalculated(): Boolean {
		return left != 0.0 && right != 0.0 && top != 0.0 && bottom != 0.0
	}

	fun isSpeedSpecified(): Boolean {
		return avgSpeed > 0
	}

	fun isHdopSpecified(): Boolean {
		return minHdop > 0
	}

	fun isColorizationTypeAvailable(colorizationType: ColorizationType): Boolean {
		return when (colorizationType) {
			ColorizationType.SPEED -> isSpeedSpecified()
			ColorizationType.ELEVATION, ColorizationType.SLOPE -> isElevationSpecified()
			else -> true
		}
	}

	fun setLatLonStart(latitude: Double, longitude: Double) {
		setGpxParameter(GpxParameter.START_LAT, latitude)
		setGpxParameter(GpxParameter.START_LON, longitude)
	}

	fun getLatLonStart(): KLatLon? {
		val lat = getGpxParameter(GpxParameter.START_LAT)
		val lon = getGpxParameter(GpxParameter.START_LON)
		return if (lat != null && lon != null) KLatLon(lat as Double, lon as Double) else null
	}

	fun getLatStart(): Any? {
		return getGpxParameter(GpxParameter.START_LAT)
	}

	fun getLonStart(): Any? {
		return getGpxParameter(GpxParameter.START_LON)
	}

	fun hasSpeedData(): Boolean {
		return hasData(POINT_SPEED)
	}

	fun hasElevationData(): Boolean {
		return hasData(POINT_ELEVATION)
	}

	fun hasElevationMetrics(): Boolean {
		return hasElevationMetricsInGpx || hasElevationData()
	}

	fun hasData(tag: String): Boolean {
		if (tag == PointAttributes.SENSOR_TAG_TEMPERATURE) {
			return availableAttributes.any { it == PointAttributes.SENSOR_TAG_TEMPERATURE_W || it == PointAttributes.SENSOR_TAG_TEMPERATURE_A }
		}
		return availableAttributes.contains(tag)
	}

	fun setHasData(tag: String, hasData: Boolean) {
		if (hasData) {
			availableAttributes.add(tag)
		} else {
			availableAttributes.remove(tag)
		}
	}

	var wptCategoryNames: String?
		get() = getGpxParameter(GpxParameter.WPT_CATEGORY_NAMES) as String?
		set(value) = setGpxParameter(GpxParameter.WPT_CATEGORY_NAMES, value)

	fun setWptCategoryNames(wptCategoryNames: Set<String>?) {
		setGpxParameter(GpxParameter.WPT_CATEGORY_NAMES,
			wptCategoryNames?.let { KAlgorithms.encodeCollection(it) })
	}

	fun getWptCategoryNamesSet(): Set<String>? {
		return wptCategoryNames?.let { KAlgorithms.decodeStringSet(it) }
	}

	fun prepareInformation(
		fileTimeStamp: Long,
		pointsAnalyser: TrackPointsAnalyser?,
		vararg splitSegments: SplitSegment
	): GpxTrackAnalysis {
		val calculations = FloatArray(1)

		var startTimeOfSingleSegment: Long = 0
		var endTimeOfSingleSegment: Long = 0

		var distanceOfSingleSegment = 0f
		var distanceMovingOfSingleSegment = 0f
		var timeMovingOfSingleSegment: Long = 0

		var totalElevation = 0f
		var elevationPoints = 0
		var speedCount = 0
		var timeDiffMillis: Long = 0
		var timeDiff = 0
		var totalSpeedSum = 0.0

		var sensorSpeedCount = 0
		var totalSensorSpeedSum = 0.0
		var sensorHrCount = 0
		var totalSensorHrSum = 0.0
		var sensorPowerCount = 0
		var totalSensorPowerSum = 0.0
		var sensorTemperatureCount = 0
		var totalSensorTemperatureSum = 0.0
		var sensorCadenceCount = 0
		var totalSensorCadenceSum = 0.0

		var _totalDistance = 0.0f

		points = 0

		pointAttributes = mutableListOf()
		availableAttributes = mutableSetOf()

		for (s in splitSegments) {
			val numberOfPoints = s.getNumberOfPoints()
			var segmentDistance = 0f
			metricEnd += s.metricEnd
			secondaryMetricEnd += s.secondaryMetricEnd
			points += numberOfPoints
			expectedRouteDuration += getExpectedRouteSegmentDuration(s)

			for (j in 0 until numberOfPoints) {
				val point = s[j]
				if (j == 0 && locationStart == null) {
					locationStart = point
					setLatLonStart(point.lat, point.lon)
				}
				if (j == numberOfPoints - 1) {
					locationEnd = point
				}

				val time = point.time
				if (time != 0L) {
					if (s.metricEnd == 0.0) {
						if (s.segment.generalSegment) {
							if (point.firstPoint) {
								startTimeOfSingleSegment = time
							} else if (point.lastPoint) {
								endTimeOfSingleSegment = time
							}
							if (startTimeOfSingleSegment != 0L && endTimeOfSingleSegment != 0L) {
								timeSpanWithoutGaps += endTimeOfSingleSegment - startTimeOfSingleSegment
								startTimeOfSingleSegment = 0
								endTimeOfSingleSegment = 0
							}
						}
					}
					startTime = minOf(startTime, time)
					endTime = maxOf(endTime, time)
				}
				updateBounds(point)

				var speed = point.speed.toFloat()
				if (speed > 0) {
					hasSpeedInTrack = true
				}
				updateHdop(point)

				if (j > 0) {
					val prev = s[j - 1]

					calculations[0] = KMapUtils.getEllipsoidDistance(prev.lat, prev.lon, point.lat, point.lon).toFloat()

					if (calculations[0] > maxDistanceBetweenPoints) {
						maxDistanceBetweenPoints = calculations[0]
					}

					_totalDistance += calculations[0]
					segmentDistance += calculations[0]
					point.distance = segmentDistance.toDouble()

					timeDiffMillis = maxOf(0, point.time - prev.time)
					timeDiff = (timeDiffMillis / 1000).toInt()

					if (!hasSpeedInTrack && speed == 0f && timeDiff > 0) {
						speed = calculations[0] / timeDiff
					}

					val timeSpecified = point.time != 0L && prev.time != 0L
					if (speed > 0 && timeSpecified && calculations[0] > timeDiffMillis / 10000f) {
						timeMoving += timeDiffMillis
						totalDistanceMoving += calculations[0]
						if (s.segment.generalSegment && !point.firstPoint) {
							timeMovingOfSingleSegment += timeDiffMillis
							distanceMovingOfSingleSegment += calculations[0]
						}
					}
				}

				minSpeed = minOf(speed, minSpeed)
				if (speed > 0 && !speed.isInfinite()) {
					totalSpeedSum += speed
					maxSpeed = maxOf(speed, maxSpeed)
					speedCount++
				}
				val isNaN = point.ele.isNaN()
				val elevation = if (isNaN) Float.NaN else point.ele.toFloat()
				if (!isNaN) {
					totalElevation += point.ele.toFloat()
					elevationPoints++
					minElevation = minOf(point.ele, minElevation)
					maxElevation = maxOf(point.ele, maxElevation)
				}

				var firstPoint = false
				var lastPoint = false
				if (s.segment.generalSegment) {
					distanceOfSingleSegment += calculations[0]
					if (point.firstPoint) {
						firstPoint = j > 0;
						distanceOfSingleSegment = 0f
						timeMovingOfSingleSegment = 0
						distanceMovingOfSingleSegment = 0f
					}
					if (point.lastPoint) {
						lastPoint = j < numberOfPoints - 1;
						totalDistanceWithoutGaps += distanceOfSingleSegment
						timeMovingWithoutGaps += timeMovingOfSingleSegment
						totalDistanceMovingWithoutGaps += distanceMovingOfSingleSegment
					}
				}
				val distance = if (j > 0) calculations[0] else 0f
				val attribute = PointAttributes(distance, timeDiff.toFloat(), firstPoint, lastPoint).apply {
					this.speed = speed
					this.elevation = elevation
				}
				addWptAttribute(point, attribute, pointsAnalyser)
				if (attribute.sensorSpeed > 0 && !attribute.sensorSpeed.isInfinite()) {
					maxSensorSpeed = maxOf(attribute.sensorSpeed, maxSensorSpeed)
					sensorSpeedCount++
					totalSensorSpeedSum += attribute.sensorSpeed
				}

				if (attribute.bikeCadence > 0) {
					maxSensorCadence = maxOf(attribute.bikeCadence, maxSensorCadence)
					sensorCadenceCount++
					totalSensorCadenceSum += attribute.bikeCadence
				}

				if (attribute.heartRate > 0) {
					val hr = attribute.heartRate.toInt()
					maxSensorHr = maxOf(hr, maxSensorHr)
					minSensorHr = if (minSensorHr == 0) hr else minOf(hr, minSensorHr)
					sensorHrCount++
					totalSensorHrSum += attribute.heartRate
				}

				val temperature = attribute.getTemperature()
				if (temperature > 0) {
					maxSensorTemperature = maxOf(temperature.toInt(), maxSensorTemperature)
					sensorTemperatureCount++
					totalSensorTemperatureSum += temperature
				}

				if (attribute.bikePower > 0) {
					maxSensorPower = maxOf(attribute.bikePower.toInt(), maxSensorPower)
					sensorPowerCount++
					totalSensorPowerSum += attribute.bikePower
				}
			}
			processElevationDiff(s)
		}


		if (!joinSegments && totalDistanceWithoutGaps > 0) {
			totalDistance = totalDistanceWithoutGaps
		} else {
			totalDistance = _totalDistance
		}

		checkUnspecifiedValues(fileTimeStamp)
		processAverageValues(totalElevation, elevationPoints, totalSpeedSum, speedCount)

		avgSensorSpeed = processAverageValue(totalSensorSpeedSum, sensorSpeedCount)
		avgSensorCadence = processAverageValue(totalSensorCadenceSum, sensorCadenceCount)
		avgSensorHr = processAverageValue(totalSensorHrSum, sensorHrCount)
		avgSensorPower = processAverageValue(totalSensorPowerSum, sensorPowerCount)
		avgSensorTemperature =
			processAverageValue(totalSensorTemperatureSum, sensorTemperatureCount)
		return this
	}

	private fun addWptAttribute(
		point: WptPt, attribute: PointAttributes, pointsAnalyser: TrackPointsAnalyser?
	) {
		if (!hasSpeedData() && attribute.speed > 0) {
			setHasData(POINT_SPEED, true)
		}
		if (!hasElevationData() && !attribute.elevation.isNaN()) {
			setHasData(POINT_ELEVATION, true)
		}
		point.attributes = attribute
		pointsAnalyser?.onAnalysePoint(this, point, attribute)
		pointAttributes.add(attribute)
	}

	private fun updateBounds(point: WptPt) {
		if (left == 0.0 && right == 0.0) {
			left = point.getLongitude()
			right = point.getLongitude()
			top = point.getLatitude()
			bottom = point.getLatitude()
		} else {
			left = minOf(left, point.getLongitude())
			right = maxOf(right, point.getLongitude())
			top = maxOf(top, point.getLatitude())
			bottom = minOf(bottom, point.getLatitude())
		}
	}

	private fun updateHdop(point: WptPt) {
		val hdop = point.hdop
		if (hdop > 0) {
			if (minHdop.isNaN() || hdop < minHdop) {
				minHdop = hdop
			}
			if (maxHdop.isNaN() || hdop > maxHdop) {
				maxHdop = hdop
			}
		}
	}

	private fun checkUnspecifiedValues(fileTimeStamp: Long) {
		if (totalDistance < 0) {
			availableAttributes.clear()
		}
		if (!isTimeSpecified()) {
			startTime = fileTimeStamp
			endTime = fileTimeStamp
		}
		if (timeSpan == 0L) {
			timeSpan = endTime - startTime
		}
	}

	fun getDurationInMs(): Long {
		return if (timeSpan > 0) timeSpan else expectedRouteDuration
	}

	fun getDurationInSeconds(): Int {
		return (getDurationInMs() / 1000f + 0.5f).toInt()
	}

	private fun getExpectedRouteSegmentDuration(segment: SplitSegment): Long {
		val routeSegments = segment.segment.routeSegments
		var result: Long = 0
		for (routeSegment in routeSegments) {
			result += (1000 * KAlgorithms.parseFloatSilently(
				routeSegment.segmentTime, 0.0f
			)).toLong()
		}
		return result
	}

	private fun processAverageValues(
		totalElevation: Float, elevationPoints: Int, totalSpeedSum: Double, speedCount: Int
	) {
		if (elevationPoints > 0) {
			avgElevation = totalElevation.toDouble() / elevationPoints
		}
		avgSpeed = if (speedCount > 0) {
			if (timeMoving > 0) {
				totalDistanceMoving / timeMoving * 1000f
			} else {
				(totalSpeedSum / speedCount).toFloat()
			}
		} else {
			-1f
		}
	}

	private fun processAverageValue(totalSum: Number, valuesCount: Int): Float {
		return if (valuesCount > 0) (totalSum.toDouble() / valuesCount).toFloat() else -1f
	}

	private fun processElevationDiff(segment: SplitSegment) {
		val approximator = getElevationApproximator(segment)
		approximator.approximate()
		val distances = approximator.getDistances()
		val elevations = approximator.getElevations()
		val indexes = approximator.getSurvivedIndexes()
		if (distances != null && elevations != null && indexes != null) {
			val elevationDiffsCalc = getElevationDiffsCalculator(distances, elevations, indexes)
			elevationDiffsCalc.calculateElevationDiffs()
			diffElevationUp += elevationDiffsCalc.getDiffElevationUp()
			diffElevationDown += elevationDiffsCalc.getDiffElevationDown()
		}
	}

	private fun getElevationApproximator(segment: SplitSegment): ElevationApproximator {
		return object : ElevationApproximator() {
			override fun getPointLatitude(index: Int): Double {
				return segment[index].lat
			}

			override fun getPointLongitude(index: Int): Double {
				return segment[index].lon
			}

			override fun getPointElevation(index: Int): Double {
				return segment[index].ele
			}

			override fun getPointsCount(): Int {
				return segment.getNumberOfPoints()
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

	interface TrackPointsAnalyser {
		fun onAnalysePoint(analysis: GpxTrackAnalysis, point: WptPt, attribute: PointAttributes)
	}
}
