package net.osmand.telegram.helpers.location

import net.osmand.Location

abstract class LocationCallback {
	open fun onLocationResult(locations: List<Location>) { }
	open fun onLocationAvailability(locationAvailable: Boolean) { }
}