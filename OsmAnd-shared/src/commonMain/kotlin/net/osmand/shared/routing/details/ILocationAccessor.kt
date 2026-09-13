package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon

/**
 * Read-only coordinate boundary used by platform route containers.
 *
 * Android and iOS implement this interface over their existing location arrays, so shared route
 * calculations can read coordinates without first allocating one [KLatLon] object per route point.
 */
interface ILocationAccessor {
	fun getLocationsCount(): Int

	fun getLatitude(index: Int): Double

	fun getLongitude(index: Int): Double
}

internal class KLatLonAccessor(
	private val locations: List<KLatLon>,
) : ILocationAccessor {
	override fun getLocationsCount(): Int = locations.size

	override fun getLatitude(index: Int): Double = locations[index].latitude

	override fun getLongitude(index: Int): Double = locations[index].longitude
}

internal class RoutePointAccessor(
	private val points: List<RoutePoint>,
) : ILocationAccessor {
	override fun getLocationsCount(): Int = points.size

	override fun getLatitude(index: Int): Double = points[index].location.latitude

	override fun getLongitude(index: Int): Double = points[index].location.longitude
}
