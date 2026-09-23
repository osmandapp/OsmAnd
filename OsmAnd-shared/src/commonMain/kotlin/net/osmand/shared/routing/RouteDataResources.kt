package net.osmand.shared.routing

import net.osmand.shared.data.KLocation
import kotlin.jvm.JvmOverloads

/**
 * The state shared by every segment of one route while it is written to or read from a gpx file.
 *
 * Segments are visited in order and each one consumes a run of [locations], so the reader has to
 * remember where the previous segment stopped. The encoding rules are collected here too, because
 * a gpx file carries one table of them for the whole route rather than one per segment.
 */
class RouteDataResources @JvmOverloads constructor(
	private val locations: MutableList<KLocation> = ArrayList(),
	private val routePointIndexes: MutableList<Int> = ArrayList()
) {

	private val rules: MutableMap<RouteTypeRule, Int> = LinkedHashMap()

	private val pointNamesMap: MutableMap<RouteDataObject, Array<IntArray?>> = HashMap()

	private var currentSegmentStartLocationIndex: Int = 0

	fun getRules(): MutableMap<RouteTypeRule, Int> = rules

	fun getLocations(): MutableList<KLocation> = locations

	fun getRoutePointIndexes(): MutableList<Int> = routePointIndexes

	fun getCurrentSegmentLocation(offset: Int): KLocation {
		val locationIndex = currentSegmentStartLocationIndex + offset
		if (locationIndex >= locations.size) {
			throw IllegalStateException("Locations index: $locationIndex out of bounds")
		}
		return locations[locationIndex]
	}

	fun getCurrentSegmentStartLocationIndex(): Int = currentSegmentStartLocationIndex

	/**
	 * Moves the cursor past the segment just handled. Consecutive segments share their meeting
	 * point, except where a route point falls on it, so the step is usually one short of the length.
	 */
	fun updateNextSegmentStartLocation(currentSegmentLength: Int) {
		val routePointIndex = routePointIndexes.indexOf(currentSegmentStartLocationIndex + currentSegmentLength)
		val overlappingNextRouteSegment = !(routePointIndex > 0 && routePointIndex < routePointIndexes.size - 1)
		currentSegmentStartLocationIndex += if (overlappingNextRouteSegment) {
			currentSegmentLength - 1
		} else {
			currentSegmentLength
		}
	}

	fun getPointNamesMap(): MutableMap<RouteDataObject, Array<IntArray?>> = pointNamesMap
}
