package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable

/** One immutable route-statistics classification and its covered distance. */
@Serializable
data class RouteStatisticElement(
	val propertyName: String,
	val userPropertyName: String,
	val color: Int,
	val distanceMeters: Float,
)

/**
 * Shared aggregate route statistics.
 * [partition] is a list so renderer-defined display order is retained during serialization.
 */
@Serializable
data class RouteStatistic(
	val name: String,
	val elements: List<RouteStatisticElement>,
	val partition: List<RouteStatisticElement>,
	val totalDistanceMeters: Float,
)
