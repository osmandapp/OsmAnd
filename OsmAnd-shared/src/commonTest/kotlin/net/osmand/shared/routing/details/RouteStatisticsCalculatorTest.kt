package net.osmand.shared.routing.details

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteStatisticsCalculatorTest {
	@Test
	fun nullRouteMatchesAndroidEmptyResult() {
		var classifications = 0

		val result = RouteStatisticsCalculator.calculate(
			route = null,
			attributeNames = listOf("routeInfo_surface"),
			classifier = classifier {
				classifications++
				null
			},
		)

		assertEquals(emptyList(), result)
		assertEquals(0, classifications)
	}

	@Test
	fun classifierRequestMatchesAndroidRenderingFilters() {
		val requests = mutableListOf<RouteAttributeClassificationRequest>()
		val segment = segment(
			distance = 4f,
			routeTypes = listOf(
				RouteTypeAttribute("surface", "asphalt"),
				RouteTypeAttribute("route", "ferry"),
				RouteTypeAttribute("highway", "primary"),
				RouteTypeAttribute("access", "yes"),
				RouteTypeAttribute("piste:type", "downhill"),
				RouteTypeAttribute("smoothness", "good"),
			),
		)

		RouteStatisticsCalculator.calculate(
			route = listOf(segment),
			attributeNames = listOf("routeInfo_surface"),
			classifier = classifier { request ->
				requests.add(request)
				RouteAttributeClassification("surface_asphalt", 0x102030)
			},
		)

		assertEquals(
			listOf(
				RouteAttributeClassificationRequest(
					attributeName = "routeInfo_surface",
					mainTag = "route",
					mainValue = "ferry",
					additional = "surface=asphalt;access=yes;smoothness=good;",
				),
			),
			requests,
		)
	}

	@Test
	fun adjacentGroupingPartitionSortingDuplicateAttributesAndUndefinedOmissionMatchAndroid() {
		val route = listOf(
			segment(2f, listOf(RouteTypeAttribute("class", "z"))),
			segment(3f, listOf(RouteTypeAttribute("class", "z"))),
			segment(5f, listOf(RouteTypeAttribute("class", "a"))),
			segment(2f, listOf(RouteTypeAttribute("class", "undefined"))),
		)
		val classifier = classifier { request ->
			if (request.attributeName == "routeInfo_missing") {
				null
			} else {
				val propertyName = request.additional.substringAfter("class=").substringBefore(';')
				if (propertyName == "undefined") null else RouteAttributeClassification(
					propertyName = propertyName,
					color = if (propertyName == "z") 30 else 20,
				)
			}
		}

		val result = RouteStatisticsCalculator.calculate(
			route = route,
			attributeNames = listOf("routeInfo_test", "routeInfo_test", "routeInfo_missing"),
			classifier = classifier,
		)

		assertEquals(2, result.size)
		assertEquals(result[0], result[1])
		assertEquals("test", result[0].name)
		assertEquals(
			listOf(
				element("z", color = 30, distance = 5f),
				element("a", color = 20, distance = 5f),
				element("undefined", color = 0, distance = 2f),
			),
			result[0].elements,
		)
		assertEquals(
			listOf(
				element("a", color = 20, distance = 5f),
				element("z", color = 30, distance = 5f),
				element("undefined", color = 0, distance = 2f),
			),
			result[0].partition,
		)
		assertEquals(12f, result[0].totalDistanceMeters)
	}

	@Test
	fun elevationInterpolationSlopeSmoothingAndDistancePartitionsMatchAndroid() {
		val requests = mutableListOf<RouteAttributeClassificationRequest>()
		val result = RouteStatisticsCalculator.calculate(
			route = listOf(
				segment(
					distance = 110f,
					heightValues = listOf(0f, 0f, 110f, 22f),
				),
			),
			attributeNames = listOf("routeInfo_steepness"),
			classifier = classifier { request ->
				requests.add(request)
				val propertyName = request.additional.substringAfter("steepness=").substringBefore(';')
				RouteAttributeClassification(
					propertyName = propertyName,
					color = if (propertyName == "17_20") 20 else 0,
				)
			},
		).single()

		assertEquals(
			listOf("steepness=-3_0;", "steepness=17_20;", "steepness=-3_0;"),
			requests.map(RouteAttributeClassificationRequest::additional),
		)
		assertEquals(
			listOf(
				element("-3_0", "-4% .. 0%", 0, 50f),
				element("17_20", "16% .. 20%", 20, 10f),
				element("-3_0", "-4% .. 0%", 0, 50f),
			),
			result.elements,
		)
		assertEquals(
			listOf(
				element("-3_0", "-4% .. 0%", 0, 100f),
				element("17_20", "16% .. 20%", 20, 10f),
			),
			result.partition,
		)
		assertEquals(110f, result.totalDistanceMeters)
	}

	@Test
	fun finalSlopeClassPreservesAndroidTwentyToOneHundredFilter() {
		val additionalFilters = mutableListOf<String>()

		RouteStatisticsCalculator.calculate(
			route = listOf(segment(110f, heightValues = listOf(0f, 0f, 110f, 44f))),
			attributeNames = listOf("routeInfo_steepness"),
			classifier = classifier { request ->
				additionalFilters.add(request.additional)
				RouteAttributeClassification(
					propertyName = request.additional.substringAfter("steepness=").substringBefore(';'),
					color = 0,
				)
			},
		)

		assertEquals(
			listOf("steepness=-3_0;", "steepness=20_100;", "steepness=-3_0;"),
			additionalFilters,
		)
	}

	private fun classifier(
		block: (RouteAttributeClassificationRequest) -> RouteAttributeClassification?,
	): RouteAttributeClassifier = object : RouteAttributeClassifier {
		override fun classify(request: RouteAttributeClassificationRequest): RouteAttributeClassification? =
			block(request)
	}

	private fun element(
		propertyName: String,
		userPropertyName: String = propertyName,
		color: Int,
		distance: Float,
	): RouteStatisticElement = RouteStatisticElement(
		propertyName = propertyName,
		userPropertyName = userPropertyName,
		color = color,
		distanceMeters = distance,
	)

	private fun segment(
		distance: Float,
		routeTypes: List<RouteTypeAttribute> = emptyList(),
		heightValues: List<Float> = emptyList(),
	): RouteSegment = RouteSegment(
		routePointStartIndex = 0,
		routePointEndIndex = 0,
		nativeStartPointIndex = 0,
		nativeEndPointIndex = 0,
		distanceMeters = distance,
		segmentTimeSeconds = 0f,
		segmentSpeedMetersPerSecond = 0f,
		roadId = 1L,
		forward = true,
		routeTypes = routeTypes,
		heightValues = heightValues,
	)
}
