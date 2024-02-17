package net.osmand.telegram.helpers.location

import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandLocationUtils.convertLocation
import java.util.LinkedList

class AndroidApiLocationServiceHelper(private val app: TelegramApplication) : LocationServiceHelper(), LocationListener {

	private var locationCallback : LocationCallback? = null
	private var networkLocationCallback : LocationCallback? = null
	private val networkListeners = LinkedList<LocationListener>()

	override fun requestLocationUpdates(callback: LocationCallback) {
		this.locationCallback = callback
		val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Lost location permissions. Couldn't request updates. $e")
			throw e
		} catch (e: IllegalArgumentException) {
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
			throw e
		}
	}

	override fun isNetworkLocationUpdatesSupported() = true

	override fun requestNetworkLocationUpdates(callback: LocationCallback) {
		this.networkLocationCallback = callback
		val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val providers = locationManager.getProviders(true)
		for (provider in providers) {
			if (provider == null || provider.equals(LocationManager.GPS_PROVIDER)) {
				continue
			}
			try {
				val networkListener = LocationListener { location ->
					val result = listOf(convertLocation(location)!!)
					networkLocationCallback?.onLocationResult(result)
				}
				locationManager.requestLocationUpdates(provider, 0, 0f, networkListener)
				networkListeners.add(networkListener)
			} catch (unlikely: SecurityException) {
				Log.d(PlatformUtil.TAG, "$provider location service permission not granted")
			} catch (exception: IllegalArgumentException) {
				Log.d(PlatformUtil.TAG, "$provider location provider not available")
			}
		}
	}

	override fun removeLocationUpdates() {
		val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.removeUpdates(this)
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Location service permission not granted", e)
			throw e
		} finally {
			while (!networkListeners.isEmpty()) {
				val listener: LocationListener? = networkListeners.poll()
				if (listener != null) {
					locationManager.removeUpdates(listener)
				}
			}
		}
	}

	override fun getFirstTimeRunDefaultLocation(callback: LocationCallback?): Location? {
		val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val providers = mutableListOf<String>()
		providers.addAll(locationManager.getProviders(true))

		val passiveFirst = providers.indexOf(LocationManager.PASSIVE_PROVIDER)
		if (passiveFirst > -1) {
			providers.add(0, providers.removeAt(passiveFirst))
		}

		for (provider in providers) {
			try {
				val location = convertLocation(locationManager.getLastKnownLocation(provider))
				if (location != null) {
					return location
				}
			} catch (e: SecurityException) {
				// location service permission not granted
			} catch (e: java.lang.IllegalArgumentException) {
				// location provider not available
			}
		}
		return null
	}

	override fun onLocationChanged(location: android.location.Location) {
		val result = listOf(convertLocation(location)!!)
		locationCallback?.onLocationResult(result)
	}

	override fun onProviderEnabled(provider: String) {
		locationCallback?.onLocationAvailability(true)
	}

	override fun onProviderDisabled(provider: String) {
		locationCallback?.onLocationAvailability(false)
	}

}