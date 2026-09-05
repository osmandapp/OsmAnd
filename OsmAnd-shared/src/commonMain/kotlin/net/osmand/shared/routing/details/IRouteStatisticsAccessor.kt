package net.osmand.shared.routing.details

/**
 * Read-only boundary for route-segment values consumed by route statistics.
 *
 * Platform route containers expose their existing segments through this interface, avoiding a
 * complete [RouteSegment] and [RouteTypeAttribute] object graph for every statistics calculation.
 */
interface IRouteStatisticsAccessor {
	fun getSegmentsCount(): Int

	fun getDistanceMeters(segmentIndex: Int): Float

	fun getHeightValues(segmentIndex: Int): FloatArray

	fun getRouteTypesCount(segmentIndex: Int): Int

	fun getRouteTypeTag(segmentIndex: Int, routeTypeIndex: Int): String?

	fun getRouteTypeValue(segmentIndex: Int, routeTypeIndex: Int): String?
}

internal class RouteSegmentStatisticsAccessor(
	private val segments: List<RouteSegment>,
) : IRouteStatisticsAccessor {
	override fun getSegmentsCount(): Int = segments.size

	override fun getDistanceMeters(segmentIndex: Int): Float = segments[segmentIndex].distanceMeters

	override fun getHeightValues(segmentIndex: Int): FloatArray = segments[segmentIndex].heightValues

	override fun getRouteTypesCount(segmentIndex: Int): Int = segments[segmentIndex].routeTypes.size

	override fun getRouteTypeTag(segmentIndex: Int, routeTypeIndex: Int): String =
		segments[segmentIndex].routeTypes[routeTypeIndex].tag

	override fun getRouteTypeValue(segmentIndex: Int, routeTypeIndex: Int): String? =
		segments[segmentIndex].routeTypes[routeTypeIndex].value
}
