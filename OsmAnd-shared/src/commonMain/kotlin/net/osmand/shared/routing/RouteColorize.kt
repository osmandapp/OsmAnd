package net.osmand.shared.routing

import net.osmand.shared.ColorPalette
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import kotlin.math.*

class RouteColorize {
	private lateinit var latitudes: DoubleArray
	private lateinit var longitudes: DoubleArray
	private lateinit var values: DoubleArray
	private lateinit var palette: ColorPalette
	private var minValue = 0.0
	private var maxValue = 0.0
	private var dataList: MutableList<RouteColorizationPoint>? = null
	private var colorizationType: ColorizationType? = null

	enum class ColorizationType {
		ELEVATION,
		SPEED,
		SLOPE,
		NONE
	}

	/**
	 * @param minValue can be NaN
	 * @param maxValue can be NaN
	 * @param palette  array {{value,color},...} - color in sRGB (decimal) format OR
	 * {{value,RED,GREEN,BLUE,ALPHA},...} - color in RGBA format
	 */
	constructor(
		latitudes: DoubleArray,
		longitudes: DoubleArray,
		values: DoubleArray,
		minValue: Double,
		maxValue: Double,
		palette: ColorPalette?
	) {
		this.latitudes = latitudes
		this.longitudes = longitudes
		this.values = values
		this.minValue = minValue
		this.maxValue = maxValue
		if (minValue.isNaN() || maxValue.isNaN()) {
			calculateMinMaxValue()
		}
		if (palette == null || palette.colors.size < 2) {
			this.palette =
				ColorPalette(ColorPalette.MIN_MAX_PALETTE, minValue, maxValue)
		} else {
			this.palette = palette
		}
	}

	/**
	 * @param type ELEVATION, SPEED, SLOPE
	 */
	constructor(
		gpxFile: GpxFile,
		type: ColorizationType,
		palette: ColorPalette?
	) : this(gpxFile, null, type, palette, 0f)

	constructor(
		gpxFile: GpxFile,
		analysis: GpxTrackAnalysis?,
		type: ColorizationType,
		palette: ColorPalette?,
		maxProfileSpeed: Float
	) {
		var analysis: GpxTrackAnalysis? = analysis
		if (!gpxFile.hasTrkPt()) {
			LOG.warn("GPX file is not consist of track points")
			return
		}
		val latList = mutableListOf<Double>()
		val lonList = mutableListOf<Double>()
		val valList = mutableListOf<Double>()
		var wptIdx = 0
		if (analysis == null) {
			val time: Long =
				if (KAlgorithms.isEmpty(gpxFile.path)) currentTimeMillis() else gpxFile.modifiedTime
			analysis = gpxFile.getAnalysis(time)
		}
		for (t in gpxFile.tracks) {
			for (ts in t.segments) {
				if (ts.generalSegment || ts.points.size < 2) {
					continue
				}
				for (p in ts.points) {
					latList.add(p.lat)
					lonList.add(p.lon)
					if (type == ColorizationType.SPEED) {
						valList.add(analysis.pointAttributes.get(wptIdx).speed.toDouble())
					} else {
						valList.add(analysis.pointAttributes.get(wptIdx).elevation.toDouble())
					}
					wptIdx++
				}
			}
		}
		colorizationType = type
		latitudes = listToArray(latList)
		longitudes = listToArray(lonList)
		values = if (type == ColorizationType.SLOPE) {
			calculateSlopesByElevations(
				latitudes,
				longitudes,
				listToArray(valList),
				SLOPE_RANGE.toDouble()
			)
		} else {
			listToArray(valList)
		}
		calculateMinMaxValue(analysis, maxProfileSpeed)
		if (type == ColorizationType.SLOPE) {
			this.palette =
				if (isValidPalette(palette)) palette!! else ColorPalette.SLOPE_PALETTE
		} else {
			this.palette = ColorPalette(
				if (isValidPalette(palette)) palette!! else ColorPalette.MIN_MAX_PALETTE,
				minValue,
				maxValue
			)
		}
	}

	private fun isValidPalette(palette: ColorPalette?): Boolean {
		return palette != null && palette.colors.size >= 2
	}

	/**
	 * Calculate slopes from elevations needs for right colorizing
	 *
	 * @param slopeRange - in what range calculate the derivative, usually we used
	 * 150 meters
	 * @return slopes array, in the begin and the end present NaN values!
	 */
	fun calculateSlopesByElevations(
		latitudes: DoubleArray, longitudes: DoubleArray, elevations: DoubleArray,
		slopeRange: Double
	): DoubleArray {
		var elevations = elevations
		correctElevations(latitudes, longitudes, elevations)
		val newElevations = elevations
		for (i in 2 until elevations.size - 2) {
			newElevations[i] =
				(elevations[i - 2] + elevations[i - 1] + elevations[i] + elevations[i + 1]
						+ elevations[i + 2])
			newElevations[i] /= 5.0
		}
		elevations = newElevations
		val slopes = DoubleArray(elevations.size)
		if (latitudes.size != longitudes.size || latitudes.size != elevations.size) {
			LOG.warn("Sizes of arrays latitudes, longitudes and values are not match")
			return slopes
		}
		val distances = DoubleArray(elevations.size)
		var totalDistance = 0.0
		distances[0] = totalDistance
		for (i in 0 until elevations.size - 1) {
			totalDistance += KMapUtils.getDistance(
				latitudes[i],
				longitudes[i], latitudes[i + 1], longitudes[i + 1]
			)
			distances[i + 1] = totalDistance
		}
		for (i in elevations.indices) {
			if (distances[i] < slopeRange / 2 || distances[i] > totalDistance - slopeRange / 2) {
				slopes[i] = Double.NaN
			} else {
				val arg = findDerivativeArguments(distances, elevations, i, slopeRange)
				slopes[i] = (arg[1] - arg[0]) / (arg[3] - arg[2])
			}
		}
		return slopes
	}

	private fun correctElevations(
		latitudes: DoubleArray,
		longitudes: DoubleArray,
		elevations: DoubleArray
	) {
		for (i in elevations.indices) {
			if (elevations[i].isNaN()) {
				var leftDist = MAX_CORRECT_ELEVATION_DISTANCE
				var rightDist = MAX_CORRECT_ELEVATION_DISTANCE
				var leftElevation = Double.NaN
				var rightElevation = Double.NaN
				var left = i - 1
				while (left > 0 && leftDist <= MAX_CORRECT_ELEVATION_DISTANCE) {
					if (!elevations[left].isNaN()) {
						val dist: Double = KMapUtils.getDistance(
							latitudes[left], longitudes[left], latitudes[i],
							longitudes[i]
						)
						if (dist < leftDist) {
							leftDist = dist
							leftElevation = elevations[left]
						} else {
							break
						}
					}
					left--
				}
				var right = i + 1
				while (right < elevations.size && rightDist <= MAX_CORRECT_ELEVATION_DISTANCE) {
					if (!elevations[right].isNaN()) {
						val dist: Double = KMapUtils.getDistance(
							latitudes[right], longitudes[right], latitudes[i],
							longitudes[i]
						)
						if (dist < rightDist) {
							rightElevation = elevations[right]
							rightDist = dist
						} else {
							break
						}
					}
					right++
				}
				if (!leftElevation.isNaN() && !rightElevation.isNaN()) {
					elevations[i] = (leftElevation + rightElevation) / 2
				} else if (leftElevation.isNaN() && !rightElevation.isNaN()) {
					elevations[i] = rightElevation
				} else if (!leftElevation.isNaN() && rightElevation.isNaN()) {
					elevations[i] = leftElevation
				} else {
					for (right in i + 1 until elevations.size) {
						if (!elevations[right].isNaN()) {
							elevations[i] = elevations[right]
							break
						}
					}
				}
			}
		}
	}

	val result: List<RouteColorizationPoint>
		get() {
			val result = mutableListOf<RouteColorizationPoint>()
			for (i in latitudes.indices) {
				result.add(
					RouteColorizationPoint(
						i,
						latitudes[i], longitudes[i], values[i]
					)
				)
			}
			setColorsToPoints(result)
			return result
		}

	fun getSimplifiedResult(simplificationZoom: Int): List<RouteColorizationPoint> {
		val simplifiedResult = simplify(simplificationZoom)
		setColorsToPoints(simplifiedResult)
		return simplifiedResult
	}

	private fun setColorsToPoints(points: List<RouteColorizationPoint>) {
		for (point in points) {
			point.primaryColor = palette.getColorByValue(point.value)
		}
	}

	fun setPalette(palette: ColorPalette?) {
		if (palette != null) {
			this.palette = palette
		}
	}

	fun simplify(simplificationZoom: Int): List<RouteColorizationPoint> {
		var dataList = dataList
		if (dataList == null) {
			dataList = mutableListOf()
			for (i in latitudes.indices) {
				dataList.add(
					RouteColorizationPoint(
						i,
						latitudes[i], longitudes[i], values[i]
					)
				)
			}
			this.dataList = dataList
		}
		val points = mutableListOf<Node>()
		val result = mutableListOf<Node>()
		for (data in dataList) {
			points.add(Node(data.lat, data.lon, data.id))
		}
		val epsilon = 2.0.pow(DEFAULT_BASE - simplificationZoom)
		result.add(points[0])
		simplifyDouglasPeucker(
			points,
			0,
			points.size - 1,
			result,
			epsilon
		)
		val simplified= mutableListOf<RouteColorizationPoint>()
		for (i in 1 until result.size) {
			val prevId: Int = result[i - 1].id
			val currentId: Int = result[i].id
			val sublist: List<RouteColorizationPoint> = dataList.subList(prevId, currentId)
			simplified.addAll(getExtremums(sublist))
		}
		val lastSurvivedPoint = result[result.size - 1]
		simplified.add(dataList[lastSurvivedPoint.id])
		return simplified
	}

	private fun simplifyDouglasPeucker(
		points: List<Node>,
		start: Int,
		end: Int,
		survivedLPoints: MutableList<Node>,
		epsilon: Double
	) {
		var dmax = Double.NEGATIVE_INFINITY
		var index = -1
		val startPt = points[start]
		val endPt = points[end]
		for (i in start + 1 until end) {
			val pt = points[i]
			val d = KMapUtils.getOrthogonalDistance(
				pt.lat, pt.lon,
				startPt.lat, startPt.lon,
				endPt.lat, endPt.lon
			)
			if (d > dmax) {
				dmax = d
				index = i
			}
		}
		if (dmax > epsilon) {
			simplifyDouglasPeucker(points, start, index, survivedLPoints, epsilon)
			simplifyDouglasPeucker(points, index, end, survivedLPoints, epsilon)
		} else {
			survivedLPoints.add(points[end])
		}
	}

	private fun getExtremums(subDataList: List<RouteColorizationPoint>): List<RouteColorizationPoint> {
		if (subDataList.size <= 2) {
			return subDataList
		}
		val result = mutableListOf<RouteColorizationPoint>()
		var min: Double
		var max: Double
		max = subDataList[0].value
		min = max
		for (pt in subDataList) {
			if (min > pt.value) {
				min = pt.value
			}
			if (max < pt.value) {
				max = pt.value
			}
		}
		val diff = max - min
		result.add(subDataList[0])
		for (i in 1 until subDataList.size - 1) {
			val prev = subDataList[i - 1].value
			val current = subDataList[i].value
			val next = subDataList[i + 1].value
			val currentData = subDataList[i]
			if (current > prev && current > next || current < prev && current < next || current < prev && current == next || current == prev && current < next || current > prev && current == next || current == prev && current > next) {
				var prevInResult: RouteColorizationPoint
				if (result.size > 0) {
					prevInResult = result[0]
					if (prevInResult.value / diff > MIN_DIFFERENCE_SLOPE) {
						result.add(currentData)
					}
				} else result.add(currentData)
			}
		}
		result.add(subDataList[subDataList.size - 1])
		return result
	}

	/**
	 * @return double[minElevation, maxElevation, minDist, maxDist]
	 */
	private fun findDerivativeArguments(
		distances: DoubleArray,
		elevations: DoubleArray,
		index: Int,
		slopeRange: Double
	): DoubleArray {
		val result = DoubleArray(4)
		val minDist = distances[index] - slopeRange / 2
		val maxDist = distances[index] + slopeRange / 2
		result[0] = Double.NaN
		result[1] = Double.NaN
		result[2] = minDist
		result[3] = maxDist
		var closestMaxIndex = -1
		var closestMinIndex = -1
		for (i in index until distances.size) {
			if (distances[i] == maxDist) {
				result[1] = elevations[i]
				break
			}
			if (distances[i] > maxDist) {
				closestMaxIndex = i
				break
			}
		}
		for (i in index downTo 0) {
			if (distances[i] == minDist) {
				result[0] = elevations[i]
				break
			}
			if (distances[i] < minDist) {
				closestMinIndex = i
				break
			}
		}
		if (closestMaxIndex > 0) {
			val diff = distances[closestMaxIndex] - distances[closestMaxIndex - 1]
			val coef = (maxDist - distances[closestMaxIndex - 1]) / diff
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient fo max must be 0..1 , coef=$coef")
			}
			result[1] =
				(1 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex]
		}
		if (closestMinIndex >= 0) {
			val diff = distances[closestMinIndex + 1] - distances[closestMinIndex]
			val coef = (minDist - distances[closestMinIndex]) / diff
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient for min must be 0..1 , coef=$coef")
			}
			result[0] =
				(1 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1]
		}
		if (result[0].isNaN() || result[1].isNaN()) {
			LOG.warn("Elevations wasn't calculated")
		}
		return result
	}

	private fun calculateMinMaxValue() {
		if (values.isEmpty()) return
		maxValue = Double.NaN
		minValue = maxValue
		for (value in values) {
			if ((maxValue.isNaN() || minValue.isNaN()) && !value.isNaN()) {
				minValue = value
				maxValue = minValue
			}
			if (minValue > value) minValue = value
			if (maxValue < value) maxValue = value
		}
	}

	private fun calculateMinMaxValue(
		analysis: GpxTrackAnalysis,
		maxProfileSpeed: Float
	) {
		calculateMinMaxValue()
		// set strict limitations for maxValue
		maxValue = getMaxValue(colorizationType, analysis, minValue, maxProfileSpeed.toDouble())
	}

	private fun listToArray(doubleList: List<Double>): DoubleArray {
		val result = DoubleArray(doubleList.size)
		for (i in doubleList.indices) {
			result[i] = doubleList[i]
		}
		return result
	}

	class RouteColorizationPoint(var id: Int, var lat: Double, var lon: Double, var value: Double) {
		var primaryColor = 0
		var secondaryColor = 0
	}

	private data class Node(val lat: Double, val lon: Double, val id: Int)

	companion object {
		private val LOG = LoggerFactory.getLogger("RouteColorize")

		var MAX_CORRECT_ELEVATION_DISTANCE = 100.0 // in meters
		var SLOPE_RANGE = 150 // 150 meters
		private const val DEFAULT_BASE = 17.2
		private const val MIN_DIFFERENCE_SLOPE = 0.05 // 5%
		fun getMinValue(
			type: ColorizationType?,
			analysis: GpxTrackAnalysis
		): Double {
			return when (type) {
				ColorizationType.SPEED -> 0.0
				ColorizationType.ELEVATION -> analysis.minElevation
				ColorizationType.SLOPE -> ColorPalette.SLOPE_MIN_VALUE
				else -> (-1.0)
			}
		}

		fun getMaxValue(
			type: ColorizationType?, analysis: GpxTrackAnalysis, minValue: Double,
			maxProfileSpeed: Double
		): Double {
			return when (type) {
				ColorizationType.SPEED -> max(analysis.maxSpeed.toDouble(), maxProfileSpeed)
				ColorizationType.ELEVATION -> max(analysis.maxElevation, minValue + 50)
				ColorizationType.SLOPE -> ColorPalette.SLOPE_MAX_VALUE
				else -> (-1.0)
			}
		}

		fun getDefaultPalette(colorizationType: ColorizationType): ColorPalette {
			return if (colorizationType == ColorizationType.SLOPE) {
				ColorPalette.SLOPE_PALETTE
			} else {
				ColorPalette.MIN_MAX_PALETTE
			}
		}
	}
}
