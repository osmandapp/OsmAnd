package net.osmand.shared.routing

import net.osmand.shared.ColorPalette
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlin.math.*

class RouteColorize {
	private lateinit var latitudes: DoubleArray
	private lateinit var longitudes: DoubleArray
	private lateinit var values: DoubleArray
	private lateinit var palette: ColorPalette
	private var fixedValues = false
	private var minValue = 0.0
	private var maxValue = 0.0
	private var dataList: MutableList<RouteColorizationPoint>? = null
	private var colorizationType: ColorizationType? = null

	enum class ColorizationType(
		val bipolar: Boolean = false
	) {
		ELEVATION,
		SPEED,
		SLOPE(bipolar = true),
		NONE;
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
		palette: ColorPalette?,
		fixedValues: Boolean
	) {
		this.latitudes = latitudes
		this.longitudes = longitudes
		this.values = values
		this.minValue = minValue
		this.maxValue = maxValue
		this.fixedValues = fixedValues
		if (minValue.isNaN() || maxValue.isNaN()) {
			calculateMinMaxValue()
		}
		if (palette == null || palette.colors.size < 2) {
			this.palette = ColorPalette(ColorPalette.MIN_MAX_PALETTE, minValue, maxValue)
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
		palette: ColorPalette?,
		fixedValues: Boolean
	) : this(gpxFile, null, type, palette, 0f, fixedValues)

	constructor(
		gpxFile: GpxFile,
		analysis: GpxTrackAnalysis?,
		type: ColorizationType,
		palette: ColorPalette?,
		maxProfileSpeed: Float,
		fixedValues: Boolean
	) {
		this.fixedValues = fixedValues
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
			SlopeCalculator.calculateSlopesByElevations(
				latitudes,
				longitudes,
				listToArray(valList),
				SLOPE_RANGE.toDouble()
			)
		} else {
			listToArray(valList)
		}
		calculateMinMaxValue(analysis, maxProfileSpeed)
		if (fixedValues) {
			this.palette = if (isValidPalette(palette)) palette!! else getDefaultPalette(type)
		} else {
			val originalPalette = if (isValidPalette(palette)) {
				palette!!
			} else {
				getDefaultRelativePalette(type)
			}
			this.palette = ColorPalette(originalPalette, minValue, maxValue, type.bipolar)
		}
	}

	private fun isValidPalette(palette: ColorPalette?): Boolean {
		return palette != null && palette.colors.size >= 2
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

		// set strict limitations for maxValue ONLY for linear (non-bipolar) types
		if (colorizationType?.bipolar == false) {
			maxValue = getMaxValue(colorizationType, analysis, minValue, maxProfileSpeed.toDouble())
		}
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

		fun getDefaultRelativePalette(colorizationType: ColorizationType): ColorPalette {
			return if (colorizationType.bipolar) {
				ColorPalette.BIPOLAR_MIN_MAX_PALETTE
			} else {
				ColorPalette.MIN_MAX_PALETTE
			}
		}
	}
}
