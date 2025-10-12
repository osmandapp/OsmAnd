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

	var lastUphill: ElevationDiffsCalculator.SlopeInfo? = null
	var lastDownhill: ElevationDiffsCalculator.SlopeInfo? = null

	private var _diffElevationUp: Double = 0.0
	private var _diffElevationDown: Double = 0.0

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

		var _totalDistance = 0f
		var _startTime = Long.MAX_VALUE
		var _endTime = Long.MIN_VALUE
		var _expectedRouteDuration = 0L
		var _points = 0
		var _timeMoving = 0L
		var _totalDistanceMoving = 0f
		var _minSpeed = Float.MAX_VALUE
		var _maxSpeed = 0f
		var _minElevation = 99999.0
		var _maxElevation = -100.0
		var _maxSensorSpeed = 0f
		var _maxSensorCadence = 0f
		var _minSensorHr = 0
		var _maxSensorHr = 0
		var _maxSensorTemperature = 0
		var _maxSensorPower = 0

		_diffElevationUp = 0.0
		_diffElevationDown = 0.0

		pointAttributes = mutableListOf()
		availableAttributes = mutableSetOf()

		for (s in splitSegments) {
			val numberOfPoints = s.getNumberOfPoints()
			var segmentDistance = 0f
			metricEnd += s.metricEnd
			secondaryMetricEnd += s.secondaryMetricEnd
			_points += numberOfPoints
			_expectedRouteDuration += getExpectedRouteSegmentDuration(s)

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
					_startTime = minOf(_startTime, time)
					_endTime = maxOf(_endTime, time)
				}
				updateBounds(point)

				var speed = point.speed.toFloat()
				if (speed > 0) {
					hasSpeedInTrack = true
				}
				updateHdop(point)

				var distance = point.attributes?.distance ?: -1f
				if (j > 0) {
					val prev = s[j - 1]
					if (distance < 0f) {
						distance = KMapUtils.getEllipsoidDistance(prev.lat, prev.lon, point.lat, point.lon).toFloat()
					}
					if (distance > maxDistanceBetweenPoints) {
						maxDistanceBetweenPoints = distance
					}
					_totalDistance += distance
					segmentDistance += distance
					point.distance = segmentDistance.toDouble()

					timeDiffMillis = maxOf(0, point.time - prev.time)
					timeDiff = (timeDiffMillis / 1000).toInt()

					if (!hasSpeedInTrack && speed == 0f && timeDiff > 0) {
						speed = distance / timeDiff
					}

					val timeSpecified = point.time != 0L && prev.time != 0L
					if (speed > 0 && timeSpecified && distance > timeDiffMillis / 10000f) {
						_timeMoving += timeDiffMillis
						_totalDistanceMoving += distance
						if (s.segment.generalSegment && !point.firstPoint) {
							timeMovingOfSingleSegment += timeDiffMillis
							distanceMovingOfSingleSegment += distance
						}
					}
				} else {
					distance = 0f
					timeDiffMillis = 0
					timeDiff = 0
				}

				_minSpeed = minOf(speed, _minSpeed)
				if (speed > 0 && !speed.isInfinite()) {
					totalSpeedSum += speed
					_maxSpeed = maxOf(speed, _maxSpeed)
					speedCount++
				}
				val isNaN = point.ele.isNaN()
				val elevation = if (isNaN) Float.NaN else point.ele.toFloat()
				if (!isNaN) {
					totalElevation += point.ele.toFloat()
					elevationPoints++
					_minElevation = minOf(point.ele, _minElevation)
					_maxElevation = maxOf(point.ele, _maxElevation)
				}

				var firstPoint = false
				var lastPoint = false
				if (s.segment.generalSegment) {
					distanceOfSingleSegment += distance
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

				var attributes = point.attributes
				if (attributes == null) {
					attributes = PointAttributes(distance, timeDiff.toFloat(), firstPoint, lastPoint).apply {
						this.speed = speed
						this.elevation = elevation
					}
					addWptAttribute(point, attributes, pointsAnalyser)
				} else {
					attributes.distance = distance
					attributes.timeDiff = timeDiff.toFloat()
					attributes.firstPoint = firstPoint
					attributes.lastPoint = lastPoint
					attributes.speed = speed
					attributes.elevation = elevation
					addWptAttribute(point, attributes, null)
				}
				if (attributes.sensorSpeed > 0 && !attributes.sensorSpeed.isInfinite()) {
					_maxSensorSpeed = maxOf(attributes.sensorSpeed, _maxSensorSpeed)
					sensorSpeedCount++
					totalSensorSpeedSum += attributes.sensorSpeed
				}

				if (attributes.bikeCadence > 0) {
					_maxSensorCadence = maxOf(attributes.bikeCadence, _maxSensorCadence)
					sensorCadenceCount++
					totalSensorCadenceSum += attributes.bikeCadence
				}

				if (attributes.heartRate > 0) {
					val hr = attributes.heartRate.toInt()
					_maxSensorHr = maxOf(hr, _maxSensorHr)
					_minSensorHr = if (_minSensorHr == 0) hr else minOf(hr, _minSensorHr)
					sensorHrCount++
					totalSensorHrSum += attributes.heartRate
				}

				val temperature = attributes.getTemperature()
				if (temperature > 0) {
					_maxSensorTemperature = maxOf(temperature.toInt(), _maxSensorTemperature)
					sensorTemperatureCount++
					totalSensorTemperatureSum += temperature
				}

				if (attributes.bikePower > 0) {
					_maxSensorPower = maxOf(attributes.bikePower.toInt(), _maxSensorPower)
					sensorPowerCount++
					totalSensorPowerSum += attributes.bikePower
				}
			}
			processElevationDiff(s)
		}

		if (!joinSegments && totalDistanceWithoutGaps > 0) {
			totalDistance = totalDistanceWithoutGaps
		} else {
			totalDistance = _totalDistance
		}
		points = _points
		expectedRouteDuration = _expectedRouteDuration
		startTime = _startTime
		endTime = _endTime
		timeMoving = _timeMoving
		totalDistanceMoving = _totalDistanceMoving
		minSpeed = _minSpeed
		maxSpeed = _maxSpeed
		minElevation = _minElevation
		maxElevation = _maxElevation
		maxSensorSpeed = _maxSensorSpeed
		maxSensorCadence = _maxSensorCadence
		minSensorHr = _minSensorHr
		maxSensorHr = _maxSensorHr
		maxSensorTemperature = _maxSensorTemperature
		maxSensorPower = _maxSensorPower
		diffElevationUp = _diffElevationUp
		diffElevationDown = _diffElevationDown

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
		point: WptPt, attributes: PointAttributes, pointsAnalyser: TrackPointsAnalyser?
	) {
		if (!hasSpeedData() && attributes.speed > 0) {
			setHasData(POINT_SPEED, true)
		}
		if (!hasElevationData() && !attributes.elevation.isNaN()) {
			setHasData(POINT_ELEVATION, true)
		}
		if (point.attributes != attributes) {
			point.attributes = attributes
		}
		pointsAnalyser?.onAnalysePoint(this, point, attributes)
		pointAttributes.add(attributes)
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
			_diffElevationUp += elevationDiffsCalc.getDiffElevationUp()
			_diffElevationDown += elevationDiffsCalc.getDiffElevationDown()

			val segLastUp = elevationDiffsCalc.getLastUphill()
			val segLastDown = elevationDiffsCalc.getLastDownhill()

			if (segLastUp != null) {
				val upDist = calculateTotalDistanceForSlope(segLastUp, indexes, distances)
				val upMaxSpeed = calculateMaxSpeedForSlope(segLastUp, segment)
				this.lastUphill = segLastUp.copy(distance = upDist, maxSpeed = upMaxSpeed)
			}
			if (segLastDown != null) {
				val downDist = calculateTotalDistanceForSlope(segLastDown, indexes, distances)
				val downMaxSpeed = calculateMaxSpeedForSlope(segLastDown, segment)
				this.lastDownhill = segLastDown.copy(distance = downDist, maxSpeed = downMaxSpeed)
			}
		}
	}

	private fun calculateTotalDistanceForSlope(
		slope: ElevationDiffsCalculator.SlopeInfo?,
		indexes: IntArray,
		distances: DoubleArray
	): Double {
		if (slope == null) return 0.0
		val startIdxPos = indexes.indexOf(slope.startPointIndex)
		val endIdxPos = indexes.indexOf(slope.endPointIndex)
		if (startIdxPos == -1 || endIdxPos == -1 || endIdxPos <= startIdxPos) return 0.0

		var total = 0.0
		for (i in (startIdxPos + 1)..endIdxPos) {
			total += distances[i]
		}
		return total
	}

	private fun calculateMaxSpeedForSlope(
		slope: ElevationDiffsCalculator.SlopeInfo?,
		segment: SplitSegment
	): Double {
		if (slope == null) return 0.0
		val startIdx = slope.startPointIndex
		val endIdx = slope.endPointIndex
		if (startIdx > endIdx) return 0.0
		var maxSpeed = 0.0
		for (i in startIdx..endIdx) {
			val pt = segment[i]
			val hasAttributes = pt.attributes != null
			val speed = if (hasAttributes) pt.attributes?.speed?.toDouble() else pt.speed
			if (speed != null) {
				if (speed > maxSpeed) {
					maxSpeed = speed.toDouble()
				}
			}
		}
		return maxSpeed
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
