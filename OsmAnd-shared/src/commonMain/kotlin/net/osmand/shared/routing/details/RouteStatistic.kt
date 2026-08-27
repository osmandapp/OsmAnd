package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable

/** Immutable copy of Android `RouteStatisticsHelper.RouteSegmentAttribute`. */
@Serializable
data class RouteStatisticElement(
	val propertyName: String,
	val userPropertyName: String,
	val color: Int,
	val distanceMeters: Float,
) {
	init {
		require(distanceMeters.isFinite() && distanceMeters >= 0f) {
			"Route statistic element distance must be finite and non-negative"
		}
	}
}

/**
 * Immutable copy of Android `RouteStatisticsHelper.RouteStatistics`.
 * [partition] is a list so the legacy `LinkedHashMap` order is retained during serialization.
 */
@Serializable
data class RouteStatistic(
	val name: String,
	val elements: List<RouteStatisticElement>,
	val partition: List<RouteStatisticElement>,
	val totalDistanceMeters: Float,
) {
	init {
		require(totalDistanceMeters.isFinite() && totalDistanceMeters >= 0f) {
			"Route statistic total distance must be finite and non-negative"
		}
	}
}
