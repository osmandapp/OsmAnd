package net.osmand.shared.routing.details

/**
 * Common port of the portable calculations in Android's
 * `net.osmand.router.RouteStatisticsHelper`.
 *
 * Attribute discovery and rendering-rule evaluation stay platform-side. Callers pass the exact
 * renderer attribute names to [calculate], and [classifier] performs current/default renderer
 * fallback through the platform boundary.
 */
object RouteStatisticsCalculator {
	const val UNDEFINED_ATTR = "undefined"
	const val ROUTE_INFO_PREFIX = "routeInfo_"

	private const val HEIGHT_STEP_METERS = 5.0
	private const val SLOPE_APPROXIMATION_METERS = 100.0
	private const val MIN_INCLINE = -101
	private const val MIN_DIVIDED_INCLINE = -20
	private const val MAX_INCLINE = 100
	private const val MAX_DIVIDED_INCLINE = 20
	private const val INCLINE_STEP = 4

	private val slopeBoundaries: IntArray
	private val slopeClasses: Array<String>
	private val mainRouteTags = setOf(
		"highway",
		"route",
		"railway",
		"aeroway",
		"aerialway",
		"piste:type",
	)

	init {
		val numberOfClasses = ((MAX_DIVIDED_INCLINE - MIN_DIVIDED_INCLINE) / INCLINE_STEP) + 3
		slopeBoundaries = IntArray(numberOfClasses)
		slopeClasses = Array(numberOfClasses) { "" }
		slopeBoundaries[0] = MIN_INCLINE
		slopeClasses[0] = "steepness=${MIN_INCLINE + 1}_$MIN_DIVIDED_INCLINE"
		for (index in 1 until numberOfClasses - 1) {
			slopeBoundaries[index] = MIN_DIVIDED_INCLINE + (index - 1) * INCLINE_STEP
			slopeClasses[index] =
				"steepness=${slopeBoundaries[index - 1] + 1}_${slopeBoundaries[index]}"
		}
		slopeBoundaries[numberOfClasses - 1] = MAX_INCLINE
		// Keep Android's current value verbatim. Its renderer XML currently uses 21_100 instead.
		slopeClasses[numberOfClasses - 1] = "steepness=${MAX_DIVIDED_INCLINE}_$MAX_INCLINE"
	}

	fun calculate(
		route: List<RouteSegment>?,
		attributeNames: List<String>,
		classifier: RouteAttributeClassifier,
	): List<RouteStatistic> {
		if (route == null) {
			return emptyList()
		}
		val segmentsWithIncline = calculateInclineRouteSegments(route)
		val result = mutableListOf<RouteStatistic>()
		for (attributeName in attributeNames) {
			val statistic = computeStatistic(segmentsWithIncline, attributeName, classifier)
			if (statistic.partition.isNotEmpty() &&
				(statistic.partition.size != 1 || statistic.partition.single().userPropertyName != UNDEFINED_ATTR)
			) {
				result.add(statistic)
			}
		}
		return result
	}

	private fun computeStatistic(
		route: List<RouteSegmentWithIncline>,
		attributeName: String,
		classifier: RouteAttributeClassifier,
	): RouteStatistic {
		val attributes = processRoute(route, attributeName, classifier)
		return RouteStatistic(
			name = if (attributeName.startsWith(ROUTE_INFO_PREFIX)) {
				attributeName.substring(ROUTE_INFO_PREFIX.length)
			} else {
				attributeName
			},
			elements = attributes.map(MutableRouteAttribute::toElement),
			partition = makePartition(attributes).map(MutableRouteAttribute::toElement),
			totalDistanceMeters = attributes.fold(0f) { distance, attribute ->
				distance + attribute.distanceMeters
			},
		)
	}

	private fun calculateInclineRouteSegments(route: List<RouteSegment>): List<RouteSegmentWithIncline> {
		val result = ArrayList<RouteSegmentWithIncline>(route.size)
		var previousHeight = 0f
		var totalHeightSamples = 0
		for (segment in route) {
			var previousSampleHeight = previousHeight
			var sampleIndex = 0
			val interpolatedHeights = if (segment.distanceMeters.toDouble() > HEIGHT_STEP_METERS) {
				FloatArray((segment.distanceMeters.toDouble() / HEIGHT_STEP_METERS).toInt() + 1)
			} else {
				null
			}
			if (interpolatedHeights != null) {
				totalHeightSamples += interpolatedHeights.size
			}

			if (segment.heightValues.isNotEmpty()) {
				var heightIndex = 2
				var cumulativeDistance = 0f
				previousSampleHeight = segment.heightValues[1]
				if (interpolatedHeights != null && sampleIndex < interpolatedHeights.size) {
					interpolatedHeights[sampleIndex++] = previousSampleHeight
				}
				while (interpolatedHeights != null &&
					sampleIndex < interpolatedHeights.size &&
					heightIndex < segment.heightValues.size
				) {
					val distance = segment.heightValues[heightIndex] + cumulativeDistance
					if (distance.toDouble() > sampleIndex * HEIGHT_STEP_METERS) {
						interpolatedHeights[sampleIndex] = if (distance == cumulativeDistance) {
							previousSampleHeight
						} else {
							(
								previousSampleHeight.toDouble() +
									(sampleIndex * HEIGHT_STEP_METERS - cumulativeDistance.toDouble()) *
									(segment.heightValues[heightIndex + 1] - previousSampleHeight).toDouble() /
									(distance - cumulativeDistance).toDouble()
							).toFloat()
						}
						sampleIndex++
					} else {
						cumulativeDistance = distance
						previousSampleHeight = segment.heightValues[heightIndex + 1]
						heightIndex += 2
					}
				}
			}
			while (interpolatedHeights != null && sampleIndex < interpolatedHeights.size) {
				interpolatedHeights[sampleIndex++] = previousSampleHeight
			}
			result.add(
				RouteSegmentWithIncline(
					segment = segment,
					interpolatedHeights = interpolatedHeights,
				),
			)
			previousHeight = previousSampleHeight
		}

		val heightSamples = FloatArray(totalHeightSamples)
		var globalIndex = 0
		for (segment in result) {
			segment.interpolatedHeights?.forEach { heightSamples[globalIndex++] = it }
		}

		val smoothingShift = (SLOPE_APPROXIMATION_METERS / (2 * HEIGHT_STEP_METERS)).toInt()
		globalIndex = 0
		var minimumSlope = Int.MAX_VALUE
		var maximumSlope = Int.MIN_VALUE
		for (segment in result) {
			val interpolatedHeights = segment.interpolatedHeights ?: continue
			val slopes = FloatArray(interpolatedHeights.size)
			segment.slopes = slopes
			for (index in slopes.indices) {
				if (globalIndex > smoothingShift && globalIndex + smoothingShift < heightSamples.size) {
					val heightDifference =
						heightSamples[globalIndex + smoothingShift] - heightSamples[globalIndex - smoothingShift]
					val slope = (heightDifference * 100).toDouble() / SLOPE_APPROXIMATION_METERS
					slopes[index] = slope.toFloat()
					minimumSlope = minOf(slope.toInt(), minimumSlope)
					maximumSlope = maxOf(slope.toInt(), maximumSlope)
				}
				globalIndex++
			}
		}

		val formattedSlopeClasses = Array(slopeBoundaries.size) { "" }
		formattedSlopeClasses[0] = formatSlope(minimumSlope, MIN_DIVIDED_INCLINE)
		formattedSlopeClasses[1] = formatSlope(minimumSlope, MIN_DIVIDED_INCLINE)
		formattedSlopeClasses[formattedSlopeClasses.lastIndex] =
			formatSlope(MAX_DIVIDED_INCLINE, maximumSlope)
		for (index in 2 until formattedSlopeClasses.lastIndex) {
			formattedSlopeClasses[index] = formatSlope(slopeBoundaries[index - 1], slopeBoundaries[index])
		}

		for (segment in result) {
			val slopes = segment.slopes ?: continue
			val slopeClassIndexes = IntArray(slopes.size)
			val slopeClassUserNames = Array(slopes.size) { "" }
			segment.slopeClassIndexes = slopeClassIndexes
			segment.slopeClassUserNames = slopeClassUserNames
			for (slopeIndex in slopes.indices) {
				for (classIndex in slopeBoundaries.indices) {
					if (slopes[slopeIndex] <= slopeBoundaries[classIndex] || classIndex == slopeBoundaries.lastIndex) {
						slopeClassIndexes[slopeIndex] = classIndex
						slopeClassUserNames[slopeIndex] = formattedSlopeClasses[classIndex]
						break
					}
				}
			}
		}
		return result
	}

	private fun processRoute(
		route: List<RouteSegmentWithIncline>,
		attributeName: String,
		classifier: RouteAttributeClassifier,
	): List<MutableRouteAttribute> {
		val routeAttributes = mutableListOf<MutableRouteAttribute>()
		var previous: MutableRouteAttribute? = null
		for (segment in route) {
			val slopeClassIndexes = segment.slopeClassIndexes
			if (slopeClassIndexes == null || slopeClassIndexes.isEmpty()) {
				val current = classify(attributeName, -1, segment.segment, classifier)
				current.distanceMeters = segment.segment.distanceMeters
				if (previous != null && previous.propertyName == current.propertyName) {
					previous.distanceMeters += current.distanceMeters
				} else {
					routeAttributes.add(current)
					previous = current
				}
			} else {
				for (index in slopeClassIndexes.indices) {
					val distance = if (index == 0) {
						(
							segment.segment.distanceMeters.toDouble() -
								HEIGHT_STEP_METERS * (slopeClassIndexes.size - 1)
						).toFloat()
					} else {
						HEIGHT_STEP_METERS.toFloat()
					}
					if (index > 0 && slopeClassIndexes[index] == slopeClassIndexes[index - 1]) {
						previous!!.distanceMeters += distance
					} else {
						val current = classify(attributeName, slopeClassIndexes[index], segment.segment, classifier)
						current.distanceMeters = distance
						if (previous != null && previous.propertyName == current.propertyName) {
							previous.distanceMeters += current.distanceMeters
						} else {
							if (current.slopeIndex == slopeClassIndexes[index]) {
								current.userPropertyName = segment.slopeClassUserNames!![index]
							}
							routeAttributes.add(current)
							previous = current
						}
					}
				}
			}
		}
		return routeAttributes
	}

	private fun classify(
		attributeName: String,
		slopeClassIndex: Int,
		segment: RouteSegment,
		classifier: RouteAttributeClassifier,
	): MutableRouteAttribute {
		val request = classificationRequest(attributeName, slopeClassIndex, segment)
		val classification = classifier.classify(request)
		return MutableRouteAttribute(
			propertyName = classification?.propertyName ?: UNDEFINED_ATTR,
			color = classification?.color ?: 0,
			slopeIndex = slopeClassIndex.takeIf { index ->
				index >= 0 && slopeClasses[index].endsWith(classification?.propertyName ?: UNDEFINED_ATTR)
			} ?: -1,
		)
	}

	private fun classificationRequest(
		attributeName: String,
		slopeClassIndex: Int,
		segment: RouteSegment,
	): RouteAttributeClassificationRequest {
		var mainTag: String? = null
		var mainValue: String? = null
		val additional = StringBuilder(
			if (slopeClassIndex >= 0) "${slopeClasses[slopeClassIndex]};" else "",
		)
		for (routeType in segment.routeTypes) {
			if (routeType.tag in mainRouteTags) {
				if (mainTag == null) {
					mainTag = routeType.tag
					mainValue = routeType.value
				}
			} else {
				additional.append(routeType.tag).append('=').append(routeType.value).append(';')
			}
		}
		return RouteAttributeClassificationRequest(
			attributeName = attributeName,
			mainTag = mainTag,
			mainValue = mainValue,
			additional = additional.toString(),
		)
	}

	private fun makePartition(attributes: List<MutableRouteAttribute>): List<MutableRouteAttribute> {
		val partitionByUserName = mutableMapOf<String, MutableRouteAttribute>()
		for (attribute in attributes) {
			val partition = partitionByUserName.getOrPut(attribute.userPropertyName) {
				attribute.copy(distanceMeters = 0f)
			}
			partition.distanceMeters += attribute.distanceMeters
		}
		val naturallyOrderedKeys = partitionByUserName.keys.sorted()
		return naturallyOrderedKeys.sortedWith { first, second ->
			when {
				first.equals(UNDEFINED_ATTR, ignoreCase = true) -> 1
				second.equals(UNDEFINED_ATTR, ignoreCase = true) -> -1
				else -> {
					val slopeComparison = partitionByUserName.getValue(first).slopeIndex.compareTo(
						partitionByUserName.getValue(second).slopeIndex,
					)
					if (slopeComparison != 0) {
						slopeComparison
					} else {
						-partitionByUserName.getValue(first).distanceMeters.compareTo(
							partitionByUserName.getValue(second).distanceMeters,
						)
					}
				}
			}
		}.map(partitionByUserName::getValue)
	}

	private fun formatSlope(slope: Int, nextSlope: Int): String = "$slope% .. $nextSlope%"

	private class RouteSegmentWithIncline(
		val segment: RouteSegment,
		val interpolatedHeights: FloatArray?,
		var slopes: FloatArray? = null,
		var slopeClassIndexes: IntArray? = null,
		var slopeClassUserNames: Array<String>? = null,
	)

	private data class MutableRouteAttribute(
		val propertyName: String,
		val color: Int,
		val slopeIndex: Int,
		var distanceMeters: Float = 0f,
		var userPropertyNameOverride: String? = null,
	) {
		var userPropertyName: String
			get() = userPropertyNameOverride ?: propertyName
			set(value) {
				userPropertyNameOverride = value
			}

		fun toElement(): RouteStatisticElement = RouteStatisticElement(
			propertyName = propertyName,
			userPropertyName = userPropertyName,
			color = color,
			distanceMeters = distanceMeters,
		)
	}
}
