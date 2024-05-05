package net.osmand.telegram.helpers.location

import net.osmand.Location

abstract class LocationServiceHelper {
	abstract fun requestLocationUpdates(callback: LocationCallback)
	abstract fun isNetworkLocationUpdatesSupported(): Boolean
	abstract fun requestNetworkLocationUpdates(callback: LocationCallback)
	abstract fun removeLocationUpdates()
	abstract fun getFirstTimeRunDefaultLocation(callback: LocationCallback?): Location?
}