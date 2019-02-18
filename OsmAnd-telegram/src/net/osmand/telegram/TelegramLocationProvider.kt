package net.osmand.telegram

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.*
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.util.MapUtils
import java.util.*

class TelegramLocationProvider(private val app: TelegramApplication) : SensorEventListener {

	private var lastTimeGPSLocationFixed: Long = 0
	private var gpsSignalLost: Boolean = false

	private var sensorRegistered = false
	private val mGravs = FloatArray(3)
	private val mGeoMags = FloatArray(3)
	private var previousCorrectionValue = 360f

	internal var avgValSin = 0f
	internal var avgValCos = 0f
	internal var lastValSin = 0f
	internal var lastValCos = 0f
	private val previousCompassValuesA = FloatArray(50)
	private val previousCompassValuesB = FloatArray(50)
	private var previousCompassIndA = 0
	private var previousCompassIndB = 0
	private var inUpdateValue = false

	@get:Synchronized
	var heading: Float? = null
		private set

	// Current screen orientation
	private var currentScreenOrientation: Int = 0

	var lastKnownLocation: net.osmand.Location? = null
		private set

	val gpsInfo = GPSInfo()

	private val locationListeners = ArrayList<TelegramLocationListener>()
	private val compassListeners = ArrayList<TelegramCompassListener>()
	private var gpsStatusListener: GpsStatus.Listener? = null
	private val mRotationM = FloatArray(9)
	private var agpsDataLastTimeDownloaded: Long = 0
	private val useMagneticFieldSensorCompass = false

	// note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
	// constant should not be changed in future
	// LocationManager.PASSIVE_PROVIDER
	// put passive provider to first place
	// find location
	val firstTimeRunDefaultLocation: net.osmand.Location?
		@SuppressLint("MissingPermission")
		get() {
			if (!AndroidUtils.isLocationPermissionAvailable(app)) {
				return null
			}
			val service = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			val ps = service.getProviders(true) ?: return null
			val providers = ArrayList(ps)
			val passiveFirst = providers.indexOf("passive")
			if (passiveFirst > -1) {
				providers.add(0, providers.removeAt(passiveFirst))
			}
			for (provider in providers) {
				val location = convertLocation(service.getLastKnownLocation(provider))
				if (location != null) {
					return location
				}
			}
			return null
		}


	private val gpsListener = object : LocationListener {
		override fun onLocationChanged(location: Location?) {
			if (location != null) {
				lastTimeGPSLocationFixed = location.time
			}
			setLocation(convertLocation(location))
		}

		override fun onProviderDisabled(provider: String) {}

		override fun onProviderEnabled(provider: String) {}

		override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
	}
	private val networkListeners = LinkedList<LocationListener>()

	val lastKnownLocationLatLon: LatLon?
		get() = if (lastKnownLocation != null) {
			LatLon(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
		} else {
			null
		}

	interface TelegramLocationListener {
		fun updateLocation(location: net.osmand.Location?)
	}

	interface TelegramCompassListener {
		fun updateCompassValue(value: Float)
	}

	@SuppressLint("MissingPermission")
	fun resumeAllUpdates() {
		val service = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (app.isInternetConnectionAvailable) {
			if (System.currentTimeMillis() - agpsDataLastTimeDownloaded > AGPS_TO_REDOWNLOAD) {
				//force an updated check for internet connectivity here before destroying A-GPS-data
				if (app.isInternetConnectionAvailable(true)) {
					redownloadAGPS()
				}
			}
		}
		if (AndroidUtils.isLocationPermissionAvailable(app)) {
			service.addGpsStatusListener(getGpsStatusListener(service))
			try {
				service.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					GPS_TIMEOUT_REQUEST.toLong(),
					GPS_DIST_REQUEST.toFloat(),
					gpsListener
				)
			} catch (e: IllegalArgumentException) {
				Log.d(PlatformUtil.TAG, "GPS location provider not available") //$NON-NLS-1$
			}

			// try to always ask for network provide : it is faster way to find location

			val providers = service.getProviders(true) ?: return
			for (provider in providers) {
				if (provider == null || provider == LocationManager.GPS_PROVIDER) {
					continue
				}
				try {
					val networkListener = NetworkListener()
					service.requestLocationUpdates(
						provider,
						GPS_TIMEOUT_REQUEST.toLong(),
						GPS_DIST_REQUEST.toFloat(),
						networkListener
					)
					networkListeners.add(networkListener)
				} catch (e: IllegalArgumentException) {
					Log.d(
						PlatformUtil.TAG,
						"$provider location provider not available"
					) //$NON-NLS-1$
				}

			}
		}

		registerOrUnregisterCompassListener(true)
	}

	fun redownloadAGPS() {
		try {
			val service = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			service.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null)
			val bundle = Bundle()
			service.sendExtraCommand("gps", "force_xtra_injection", bundle)
			service.sendExtraCommand("gps", "force_time_injection", bundle)
			agpsDataLastTimeDownloaded = System.currentTimeMillis()
		} catch (e: Exception) {
			agpsDataLastTimeDownloaded = 0L
			e.printStackTrace()
		}

	}

	private fun getGpsStatusListener(service: LocationManager): GpsStatus.Listener {
		gpsStatusListener = object : GpsStatus.Listener {
			private var gpsStatus: GpsStatus? = null

			@SuppressLint("MissingPermission")
			override fun onGpsStatusChanged(event: Int) {
				try {
					gpsStatus = service.getGpsStatus(gpsStatus)
				} catch (e: Exception) {
					e.printStackTrace()
				}
				updateGPSInfo(gpsStatus)
				updateLocation(lastKnownLocation)
			}
		}
		return gpsStatusListener!!
	}

	private fun updateGPSInfo(s: GpsStatus?) {
		var fixed = false
		var n = 0
		var u = 0
		if (s != null) {
			val iterator = s.satellites.iterator()
			while (iterator.hasNext()) {
				val g = iterator.next()
				n++
				if (g.usedInFix()) {
					u++
					fixed = true
				}
			}
		}
		gpsInfo.fixed = fixed
		gpsInfo.foundSatellites = n
		gpsInfo.usedSatellites = u
	}

	fun updateScreenOrientation(orientation: Int) {
		currentScreenOrientation = orientation
	}

	fun addLocationListener(listener: TelegramLocationListener) {
		if (!locationListeners.contains(listener)) {
			locationListeners.add(listener)
		}
	}

	fun removeLocationListener(listener: TelegramLocationListener) {
		locationListeners.remove(listener)
	}

	fun addCompassListener(listener: TelegramCompassListener) {
		if (!compassListeners.contains(listener)) {
			compassListeners.add(listener)
		}
	}

	fun removeCompassListener(listener: TelegramCompassListener) {
		compassListeners.remove(listener)
	}

	@Synchronized
	fun registerOrUnregisterCompassListener(register: Boolean) {
		if (sensorRegistered && !register) {
			Log.d(PlatformUtil.TAG, "Disable sensor") //$NON-NLS-1$
			(app.getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
			sensorRegistered = false
			heading = null
		} else if (!sensorRegistered && register) {
			Log.d(PlatformUtil.TAG, "Enable sensor") //$NON-NLS-1$
			val sensorMgr = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
			if (useMagneticFieldSensorCompass) {
				var s: Sensor? = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor accelerometer could not be enabled")
				}
				s = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor magnetic field could not be enabled")
				}
			} else {
				val s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor orientation could not be enabled")
				}
			}
			sensorRegistered = true
		}
	}

	override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent) {
		// Attention : sensor produces a lot of events & can hang the system
		if (inUpdateValue) {
			return
		}
		synchronized(this) {
			if (!sensorRegistered) {
				return
			}
			inUpdateValue = true
			try {
				var `val` = 0f
				when (event.sensor.type) {
					Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, mGravs, 0, 3)
					Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, mGeoMags, 0, 3)
					Sensor.TYPE_ORIENTATION -> `val` = event.values[0]
					else -> return
				}
				if (useMagneticFieldSensorCompass) {
					if (mGravs != null && mGeoMags != null) {
						val success =
							SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags)
						if (!success) {
							return
						}
						val orientation = SensorManager.getOrientation(mRotationM, FloatArray(3))
						`val` = Math.toDegrees(orientation[0].toDouble()).toFloat()
					} else {
						return
					}
				}
				`val` = calcScreenOrientationCorrection(`val`)
				`val` = calcGeoMagneticCorrection(`val`)

				val valRad = (`val` / 180f * Math.PI).toFloat()
				lastValSin = Math.sin(valRad.toDouble()).toFloat()
				lastValCos = Math.cos(valRad.toDouble()).toFloat()

				avgValSin = lastValSin
				avgValCos = lastValCos

				updateCompassVal()
			} finally {
				inUpdateValue = false
			}
		}
	}

	private fun calcGeoMagneticCorrection(value: Float): Float {
		var res = value
		if (previousCorrectionValue == 360f && lastKnownLocation != null) {
			val l = lastKnownLocation
			val gf = GeomagneticField(
				l!!.latitude.toFloat(), l.longitude.toFloat(), l.altitude.toFloat(),
				System.currentTimeMillis()
			)
			previousCorrectionValue = gf.declination
		}
		if (previousCorrectionValue != 360f) {
			res += previousCorrectionValue
		}
		return res
	}

	private fun calcScreenOrientationCorrection(value: Float): Float {
		var res = value
		when (currentScreenOrientation) {
			1 -> res += 90f
			2 -> res += 180f
			3 -> res -= 90f
		}
		return res
	}

	private fun filterCompassValue() {
		if (heading == null && previousCompassIndA == 0) {
			Arrays.fill(previousCompassValuesA, lastValSin)
			Arrays.fill(previousCompassValuesB, lastValCos)
			avgValSin = lastValSin
			avgValCos = lastValCos
		} else {
			val l = previousCompassValuesA.size
			previousCompassIndA = (previousCompassIndA + 1) % l
			previousCompassIndB = (previousCompassIndB + 1) % l
			// update average
			avgValSin += (-previousCompassValuesA[previousCompassIndA] + lastValSin) / l
			previousCompassValuesA[previousCompassIndA] = lastValSin
			avgValCos += (-previousCompassValuesB[previousCompassIndB] + lastValCos) / l
			previousCompassValuesB[previousCompassIndB] = lastValCos
		}
	}

	private fun updateCompassVal() {
		heading = getAngle(avgValSin, avgValCos)
		for (c in compassListeners) {
			c.updateCompassValue(heading!!)
		}
	}

	private fun getAngle(sinA: Float, cosA: Float) = MapUtils.unifyRotationTo360(
		(Math.atan2(sinA.toDouble(), cosA.toDouble()) * 180 / Math.PI).toFloat()
	)

	private fun updateLocation(loc: net.osmand.Location?) {
		for (l in locationListeners) {
			l.updateLocation(loc)
		}
	}

	private fun useOnlyGPS() =
		System.currentTimeMillis() - lastTimeGPSLocationFixed < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS

	// Working with location checkListeners
	private inner class NetworkListener : LocationListener {

		override fun onLocationChanged(location: Location) {
			if (!useOnlyGPS()) {
				setLocation(convertLocation(location))
			}
		}

		override fun onProviderDisabled(provider: String) {}

		override fun onProviderEnabled(provider: String) {}

		override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
	}

	private fun stopLocationRequests() {
		val service = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		service.removeGpsStatusListener(gpsStatusListener)
		service.removeUpdates(gpsListener)
		while (!networkListeners.isEmpty()) {
			service.removeUpdates(networkListeners.poll())
		}
	}

	fun pauseAllUpdates() {
		stopLocationRequests()
		registerOrUnregisterCompassListener(false)
	}

	private fun setLocation(location: net.osmand.Location?) {
		if (location == null) {
			updateGPSInfo(null)
		}
		if (location != null) {
			if (gpsSignalLost) {
				gpsSignalLost = false
			}
		}
		this.lastKnownLocation = location

		// Update information
		updateLocation(this.lastKnownLocation)
	}

	fun checkIfLastKnownLocationIsValid() {
		val loc = lastKnownLocation
		if (loc != null && System.currentTimeMillis() - loc.time > INTERVAL_TO_CLEAR_SET_LOCATION) {
			setLocation(null)
		}
	}

	class GPSInfo {
		var foundSatellites = 0
		var usedSatellites = 0
		var fixed = false
	}

	companion object {

		private const val INTERVAL_TO_CLEAR_SET_LOCATION = 30 * 1000

		private const val GPS_TIMEOUT_REQUEST = 0
		private const val GPS_DIST_REQUEST = 0
		private const val NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000
		private const val AGPS_TO_REDOWNLOAD = 16L * 60 * 60 * 1000 // 16 hours

		fun convertLocation(l: Location?): net.osmand.Location? {
			if (l == null) {
				return null
			}
			val r = net.osmand.Location(l.provider)
			r.latitude = l.latitude
			r.longitude = l.longitude
			r.time = l.time
			if (l.hasAccuracy()) {
				r.accuracy = l.accuracy
			}
			if (l.hasSpeed()) {
				r.speed = l.speed
			}
			if (l.hasAltitude()) {
				r.altitude = l.altitude
			}
			if (l.hasBearing()) {
				r.bearing = l.bearing
			}
			return r
		}
	}
}
