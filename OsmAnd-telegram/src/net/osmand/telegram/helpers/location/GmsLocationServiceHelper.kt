package net.osmand.telegram.helpers.location

import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandLocationUtils.convertLocation

class GmsLocationServiceHelper(app: TelegramApplication): LocationServiceHelper() {

	// FusedLocationProviderClient - Main class for receiving location updates.
	private val fusedLocationProviderClient : FusedLocationProviderClient

	// LocationRequest - Requirements for the location updates, i.e., how often you should receive
	// updates, the priority, etc.
	private val fusedLocationRequest : LocationRequest

	// Native LocationCallback from Google Services
	// Called when FusedLocationProviderClient has a new Location.
	private val fusedLocationCallback : com.google.android.gms.location.LocationCallback

	// Callback to return results of calling helper methods to external requesters.
	private var locationCallback: LocationCallback? = null

	init {
		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(app)

		// Sets the desired interval for active location updates. This interval is inexact. You
		// may not receive updates at all if no location sources are available, or you may
		// receive them less frequently than requested. You may also receive updates more
		// frequently than requested if other applications are requesting location at a more
		// frequent interval.
		//
		// IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
		// targetSdkVersion) may receive updates less frequently than this interval when the app
		// is no longer in the foreground.
		val interval = 1000L

		fusedLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).build()

		fusedLocationCallback = object  : com.google.android.gms.location.LocationCallback() {

			override fun onLocationResult(locationResult: LocationResult) {
				val lastLocation = locationResult.lastLocation
				val result: List<Location> = if (lastLocation != null) listOf(convertLocation(lastLocation)!!) else emptyList()
				locationCallback?.onLocationResult(result)
			}

			override fun onLocationAvailability(locationAvailability: LocationAvailability) {
				locationCallback?.onLocationAvailability(locationAvailability.isLocationAvailable)
			}
		}
	}

	override fun requestLocationUpdates(callback: LocationCallback) {
		this.locationCallback = callback
		try {
			fusedLocationProviderClient.requestLocationUpdates(
				fusedLocationRequest, fusedLocationCallback, Looper.myLooper())
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Lost location permissions. Couldn't request updates. $e")
			throw e
		} catch (e: IllegalArgumentException) {
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
			throw e
		}
	}

	override fun isNetworkLocationUpdatesSupported() = false

	override fun requestNetworkLocationUpdates(callback: LocationCallback) {}

	override fun removeLocationUpdates() {
		try {
			fusedLocationProviderClient.removeLocationUpdates(fusedLocationCallback)
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Lost location permissions. Couldn't remove updates. $e")
			throw e
		}
	}

	override fun getFirstTimeRunDefaultLocation(callback: LocationCallback?): Location? {
		locationCallback ?: return null
		try {
			val lastLocation = fusedLocationProviderClient.lastLocation
			lastLocation.addOnSuccessListener { loc: android.location.Location? ->
				val result = if (loc != null) listOf(convertLocation(loc)!!) else emptyList()
				locationCallback!!.onLocationResult(result)
			}
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Location service permission not granted")
		} catch (e: IllegalArgumentException) {
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
		}
		return null
	}
}