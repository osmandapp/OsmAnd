package net.osmand.shared.data

/**
 * Where an amenity found by a search along a path sits relative to that path.
 *
 * A copy of `Amenity.AmenityRoutePoint` in OsmAnd-java, lifted out to its own file.
 */
class AmenityRoutePoint {

	var deviateDistance: Double = 0.0
	var deviationDirectionRight: Boolean = false
	var pointA: KLocation? = null
	var pointB: KLocation? = null
}
